import json
import os
import subprocess
import sys
import threading
import time
import argparse
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Callable, Dict, Optional

import requests

try:
	import tomllib  # Python 3.11+
except ModuleNotFoundError:
	print("Python 3.11+ is required (tomllib missing).", file=sys.stderr)
	sys.exit(1)


DEFAULT_BASE_URL = "https://lichess.org"


class JsonlLogger:
	def __init__(self, file_path: str, also_stdout: bool = True):
		self.file_path = file_path
		self.also_stdout = also_stdout
		self._lock = threading.Lock()

	def log(self, category: str, event: str, **fields: Any) -> None:
		def _normalize(value: Any) -> Any:
			if isinstance(value, bytes):
				return value.decode("utf-8", errors="replace")
			if isinstance(value, dict):
				return {k: _normalize(v) for k, v in value.items()}
			if isinstance(value, list):
				return [_normalize(v) for v in value]
			if isinstance(value, tuple):
				return [_normalize(v) for v in value]
			return value

		payload = {
			"ts_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
			"category": category,
			"event": event,
			**{k: _normalize(v) for k, v in fields.items()},
		}
		line = json.dumps(payload, ensure_ascii=True)
		with self._lock:
			with open(self.file_path, "a", encoding="utf-8") as f:
				f.write(line + "\n")
			if self.also_stdout:
				print(line)


@dataclass
class AppConfig:
	target_username: str
	clock_limit_seconds: int
	clock_increment_seconds: int
	engine_path: str
	default_depth: int
	fixed_think_ms: int
	min_think_ms: int
	safety_buffer_ms: int
	log_file: str
	base_url: str
	challenge_color: str


def load_config(path: str) -> AppConfig:
	with open(path, "rb") as f:
		raw = tomllib.load(f)

	lichess = raw.get("lichess", {})
	challenge = raw.get("challenge", {})
	engine = raw.get("engine", {})
	timing = raw.get("timing", {})
	logging_cfg = raw.get("logging", {})

	target_username = str(challenge.get("target_username", "")).strip()
	if not target_username:
		raise ValueError("Set challenge.target_username in lichess_config.toml")

	clock_limit_seconds = int(challenge.get("clock_limit_seconds", 300))
	clock_increment_seconds = int(challenge.get("clock_increment_seconds", 2))
	challenge_color = str(challenge.get("color", "random")).strip().lower()
	if challenge_color not in {"white", "black", "random"}:
		raise ValueError("challenge.color must be white, black, or random")

	engine_path = str(engine.get("path", "./uci_mode.exe")).strip()
	default_depth = int(engine.get("default_depth", 4))
	if default_depth <= 0:
		raise ValueError("engine.default_depth must be > 0")

	fixed_think_ms = int(timing.get("fixed_think_ms", 400))
	min_think_ms = int(timing.get("min_think_ms", 50))
	safety_buffer_ms = int(timing.get("safety_buffer_ms", 200))

	log_file = str(logging_cfg.get("jsonl_file", "springer_lichess_log.jsonl")).strip()
	base_url = str(lichess.get("base_url", DEFAULT_BASE_URL)).strip().rstrip("/")

	return AppConfig(
		target_username=target_username,
		clock_limit_seconds=clock_limit_seconds,
		clock_increment_seconds=clock_increment_seconds,
		engine_path=engine_path,
		default_depth=default_depth,
		fixed_think_ms=fixed_think_ms,
		min_think_ms=min_think_ms,
		safety_buffer_ms=safety_buffer_ms,
		log_file=log_file,
		base_url=base_url,
		challenge_color=challenge_color,
	)


class LichessClient:
	def __init__(self, cfg: AppConfig, logger: JsonlLogger, token: str):
		self.cfg = cfg
		self.logger = logger
		self.session = requests.Session()
		self.session.headers.update({
			"Authorization": f"Bearer {token}",
			"Accept": "application/x-ndjson, application/json",
		})

	def _url(self, path: str) -> str:
		return f"{self.cfg.base_url}{path}"

	def get_account(self) -> Dict[str, Any]:
		url = self._url("/api/account")
		self.logger.log("lichess_api", "request", method="GET", path="/api/account")
		resp = self.session.get(url, timeout=30)
		self.logger.log("lichess_api", "response", method="GET", path="/api/account", status=resp.status_code)
		resp.raise_for_status()
		return resp.json()

	def create_challenge(self) -> Dict[str, Any]:
		path = f"/api/challenge/{self.cfg.target_username}"
		url = self._url(path)
		payload = {
			"rated": "false",
			"clock.limit": str(self.cfg.clock_limit_seconds),
			"clock.increment": str(self.cfg.clock_increment_seconds),
			"color": self.cfg.challenge_color,
		}
		self.logger.log("lichess_api", "request", method="POST", path=path, payload=payload)
		resp = self.session.post(url, data=payload, timeout=30)
		self.logger.log("lichess_api", "response", method="POST", path=path, status=resp.status_code)
		resp.raise_for_status()
		return resp.json()

	def make_move(self, game_id: str, move_uci: str) -> None:
		path = f"/api/bot/game/{game_id}/move/{move_uci}"
		url = self._url(path)
		self.logger.log("lichess_api", "request", method="POST", path=path)
		resp = self.session.post(url, timeout=30)
		self.logger.log(
			"lichess_api",
			"response",
			method="POST",
			path=path,
			status=resp.status_code,
			body=resp.text,
		)
		resp.raise_for_status()

	def resign_game(self, game_id: str) -> None:
		path = f"/api/bot/game/{game_id}/resign"
		url = self._url(path)
		self.logger.log("lichess_api", "request", method="POST", path=path)
		resp = self.session.post(url, timeout=30)
		self.logger.log("lichess_api", "response", method="POST", path=path, status=resp.status_code)
		resp.raise_for_status()

	def stream_event_lines(self):
		path = "/api/stream/event"
		url = self._url(path)
		self.logger.log("lichess_stream", "connect", stream="event", path=path)
		with self.session.get(url, stream=True, timeout=60) as resp:
			self.logger.log("lichess_stream", "connected", stream="event", status=resp.status_code)
			resp.raise_for_status()
			for raw in resp.iter_lines(decode_unicode=True):
				if raw is None:
					continue
				if isinstance(raw, bytes):
					raw = raw.decode("utf-8", errors="replace")
				line = raw.strip()
				if not line:
					continue
				yield line

	def stream_game_lines(self, game_id: str):
		path = f"/api/bot/game/stream/{game_id}"
		url = self._url(path)
		self.logger.log("lichess_stream", "connect", stream="game", game_id=game_id, path=path)
		with self.session.get(url, stream=True, timeout=60) as resp:
			self.logger.log("lichess_stream", "connected", stream="game", game_id=game_id, status=resp.status_code)
			resp.raise_for_status()
			for raw in resp.iter_lines(decode_unicode=True):
				if raw is None:
					continue
				if isinstance(raw, bytes):
					raw = raw.decode("utf-8", errors="replace")
				line = raw.strip()
				if not line:
					continue
				yield line


class UciEngine:
	def __init__(self, cfg: AppConfig, logger: JsonlLogger):
		self.cfg = cfg
		self.logger = logger
		self.proc: Optional[subprocess.Popen[str]] = None

	def start(self) -> None:
		if not os.path.exists(self.cfg.engine_path):
			raise FileNotFoundError(f"Engine executable not found: {self.cfg.engine_path}")
		self.proc = subprocess.Popen(
			[self.cfg.engine_path],
			stdin=subprocess.PIPE,
			stdout=subprocess.PIPE,
			stderr=subprocess.PIPE,
			text=True,
			bufsize=1,
		)
		self.logger.log("uci_process", "started", engine_path=self.cfg.engine_path, pid=self.proc.pid)

	def stop(self) -> None:
		if self.proc is None:
			return
		try:
			self.send("quit")
		except Exception:
			pass
		try:
			self.proc.wait(timeout=2)
		except Exception:
			self.proc.kill()
		self.logger.log("uci_process", "stopped", returncode=self.proc.returncode)
		self.proc = None

	def send(self, cmd: str) -> None:
		if self.proc is None or self.proc.stdin is None:
			raise RuntimeError("Engine process is not running")
		self.logger.log("uci_stdin", "send", line=cmd)
		self.proc.stdin.write(cmd + "\n")
		self.proc.stdin.flush()

	def read_line(self, timeout_seconds: float = 30.0) -> str:
		if self.proc is None or self.proc.stdout is None:
			raise RuntimeError("Engine process is not running")
		start = time.time()
		while True:
			line = self.proc.stdout.readline()
			if line:
				line = line.strip()
				self.logger.log("uci_stdout", "recv", line=line)
				return line
			if time.time() - start > timeout_seconds:
				raise TimeoutError("Timed out waiting for UCI output")
			time.sleep(0.01)

	def wait_for_token(self, token: str, timeout_seconds: float = 30.0) -> None:
		start = time.time()
		while True:
			line = self.read_line(timeout_seconds=timeout_seconds)
			if line == token:
				return
			if time.time() - start > timeout_seconds:
				raise TimeoutError(f"Timed out waiting for token: {token}")

	def init_uci(self) -> None:
		self.send("uci")
		self.wait_for_token("uciok", timeout_seconds=30)
		self.send("isready")
		self.wait_for_token("readyok", timeout_seconds=30)
		self.send("ucinewgame")
		self.send("isready")
		self.wait_for_token("readyok", timeout_seconds=30)

	def compute_bestmove(self, position_cmd: str, movetime_ms: int, depth: int) -> str:
		self.send(position_cmd)
		if movetime_ms > 0:
			self.send(f"go movetime {movetime_ms}")
		else:
			self.send(f"go depth {depth}")

		while True:
			line = self.read_line(timeout_seconds=max(5.0, movetime_ms / 1000.0 + 5.0))
			if line.startswith("bestmove "):
				parts = line.split()
				if len(parts) >= 2:
					return parts[1]
				return "0000"


class OperatorControl:
	def __init__(self, logger: JsonlLogger, on_resign: Callable[[], None]):
		self.logger = logger
		self.on_resign = on_resign
		self.stop_event = threading.Event()
		self.thread = threading.Thread(target=self._run, daemon=True)

	def start(self) -> None:
		self.thread.start()

	def stop(self) -> None:
		self.stop_event.set()

	def _run(self) -> None:
		self.logger.log(
			"operator",
			"commands_ready",
			help="Type 'r' or 'resign' then Enter to resign the active game.",
		)
		while not self.stop_event.is_set():
			try:
				cmd = input().strip().lower()
			except EOFError:
				self.logger.log("operator", "stdin_closed")
				return
			except Exception as exc:
				self.logger.log("operator", "input_error", message=str(exc))
				return

			if cmd in {"r", "resign"}:
				self.logger.log("operator", "resign_requested", command=cmd)
				try:
					self.on_resign()
				except Exception as exc:
					self.logger.log("operator", "resign_request_error", message=str(exc))
			elif cmd:
				self.logger.log("operator", "unknown_command", command=cmd)


def move_list_from_state(state_moves: str) -> list[str]:
	moves = state_moves.strip()
	if not moves:
		return []
	return moves.split()


def side_to_move_from_ply(ply_count: int) -> str:
	return "white" if ply_count % 2 == 0 else "black"


def compute_movetime_ms(remaining_ms: Optional[int], cfg: AppConfig) -> int:
	if remaining_ms is None:
		return cfg.fixed_think_ms
	budget = min(cfg.fixed_think_ms, max(cfg.min_think_ms, remaining_ms - cfg.safety_buffer_ms))
	return max(cfg.min_think_ms, budget)


def build_position_command(initial_fen: str, moves: list[str]) -> str:
	if initial_fen == "startpos":
		if moves:
			return "position startpos moves " + " ".join(moves)
		return "position startpos"
	if moves:
		return f"position fen {initial_fen} moves {' '.join(moves)}"
	return f"position fen {initial_fen}"


def parse_json_line(logger: JsonlLogger, line: str, stream_name: str) -> Dict[str, Any]:
	logger.log("lichess_stream", "line", stream=stream_name, raw=line)
	parsed = json.loads(line)
	logger.log(
		"lichess_stream",
		"parsed",
		stream=stream_name,
		type=parsed.get("type", "unknown"),
		keys=sorted(parsed.keys()),
	)
	return parsed


def wait_for_challenge_result(client: LichessClient, logger: JsonlLogger, challenge_id: str) -> Optional[str]:
	logger.log("challenge", "waiting", challenge_id=challenge_id)
	for line in client.stream_event_lines():
		event = parse_json_line(logger, line, "event")
		event_type = event.get("type")

		if event_type == "challengeDeclined":
			challenge = event.get("challenge", {})
			if challenge.get("id") == challenge_id:
				logger.log("challenge", "declined", challenge_id=challenge_id, reason=event.get("declineReason"))
				return None

		if event_type == "challengeCanceled":
			challenge = event.get("challenge", {})
			if challenge.get("id") == challenge_id:
				logger.log("challenge", "canceled", challenge_id=challenge_id)
				return None

		if event_type == "gameStart":
			game = event.get("game", {})
			game_id = game.get("id")
			if game_id:
				logger.log("challenge", "accepted", challenge_id=challenge_id, game_id=game_id)
				return game_id

	logger.log("challenge", "event_stream_closed", challenge_id=challenge_id)
	return None


def play_single_game(client: LichessClient, engine: UciEngine, logger: JsonlLogger, game_id: str, our_username: str, cfg: AppConfig) -> None:
	initial_fen = "startpos"
	our_color: Optional[str] = None
	last_moves_text = ""
	game_finished = False
	pending_move: Optional[str] = None
	pending_move_ply: Optional[int] = None
	pending_move_retries = 0
	max_pending_move_retries = 3

	for line in client.stream_game_lines(game_id):
		event = parse_json_line(logger, line, "game")
		event_type = event.get("type")

		if event_type == "gameFull":
			white = event.get("white", {})
			black = event.get("black", {})
			initial_fen = event.get("initialFen", "startpos")

			white_name = str(white.get("name", "")).lower()
			black_name = str(black.get("name", "")).lower()
			our_name = our_username.lower()

			if white_name == our_name:
				our_color = "white"
			elif black_name == our_name:
				our_color = "black"

			state = event.get("state", {})
			last_moves_text = str(state.get("moves", ""))
			logger.log(
				"game",
				"full",
				game_id=game_id,
				our_color=our_color,
				initial_fen=initial_fen,
				moves=last_moves_text,
			)

			if str(state.get("status", "started")) != "started":
				game_finished = True

		elif event_type == "gameState":
			last_moves_text = str(event.get("moves", ""))
			status = str(event.get("status", "started"))
			logger.log("game", "state", game_id=game_id, status=status, moves=last_moves_text)
			if status != "started":
				game_finished = True

		else:
			continue

		if game_finished:
			logger.log("game", "finished", game_id=game_id)
			return

		if our_color is None:
			continue

		move_list = move_list_from_state(last_moves_text)
		side_to_move = side_to_move_from_ply(len(move_list))

		if pending_move is not None and pending_move_ply is not None:
			if len(move_list) > pending_move_ply:
				if move_list[-1] == pending_move:
					logger.log(
						"game",
						"pending_move_confirmed",
						game_id=game_id,
						move=pending_move,
						retries=pending_move_retries,
					)
				else:
					logger.log(
						"game",
						"pending_move_superseded",
						game_id=game_id,
						move=pending_move,
						retries=pending_move_retries,
						last_move=move_list[-1],
					)
				pending_move = None
				pending_move_ply = None
				pending_move_retries = 0

		if side_to_move != our_color:
			continue

		remaining_ms = None
		if event_type == "gameState":
			if our_color == "white":
				remaining_ms = event.get("wtime")
			else:
				remaining_ms = event.get("btime")
		elif event_type == "gameFull":
			state = event.get("state", {})
			if our_color == "white":
				remaining_ms = state.get("wtime")
			else:
				remaining_ms = state.get("btime")

		movetime_ms = compute_movetime_ms(remaining_ms, cfg)
		position_cmd = build_position_command(initial_fen, move_list)

		logger.log(
			"decision",
			"compute_move",
			game_id=game_id,
			our_color=our_color,
			side_to_move=side_to_move,
			remaining_ms=remaining_ms,
			movetime_ms=movetime_ms,
			move_count=len(move_list),
		)

		if pending_move is not None and pending_move_ply == len(move_list):
			bestmove = pending_move
			logger.log(
				"decision",
				"retry_pending_move",
				game_id=game_id,
				bestmove=bestmove,
				retry_count=pending_move_retries,
			)
		else:
			bestmove = engine.compute_bestmove(position_cmd, movetime_ms, cfg.default_depth)
		logger.log("decision", "engine_bestmove", game_id=game_id, bestmove=bestmove)

		if bestmove in {"", "0000", "(none)"}:
			logger.log("decision", "no_move_from_engine", game_id=game_id)
			return

		is_retrying_pending = pending_move is not None and pending_move_ply == len(move_list) and pending_move == bestmove

		try:
			client.make_move(game_id, bestmove)
			if pending_move is not None and pending_move == bestmove and pending_move_ply == len(move_list):
				logger.log(
					"game",
					"pending_move_posted",
					game_id=game_id,
					move=bestmove,
					retries=pending_move_retries,
				)
			pending_move = None
			pending_move_ply = None
			pending_move_retries = 0
		except requests.HTTPError as exc:
			status_code = exc.response.status_code if exc.response is not None else None
			response_text = exc.response.text if exc.response is not None else ""
			logger.log(
				"game",
				"move_rejected",
				game_id=game_id,
				bestmove=bestmove,
				status=status_code,
				response=response_text,
			)
			if is_retrying_pending:
				logger.log(
					"game",
					"pending_move_retry_rejected",
					game_id=game_id,
					bestmove=bestmove,
					status=status_code,
					action="wait_for_next_state",
				)
				continue
			if status_code == 400:
				logger.log("game", "finished_or_desynced", game_id=game_id, action="stop_game_loop")
				return
			raise
		except requests.RequestException as exc:
			if pending_move is not None and pending_move == bestmove and pending_move_ply == len(move_list):
				pending_move_retries += 1
			else:
				pending_move = bestmove
				pending_move_ply = len(move_list)
				pending_move_retries = 1

			logger.log(
				"game",
				"move_post_transient_error",
				game_id=game_id,
				bestmove=bestmove,
				retry_count=pending_move_retries,
				max_retries=max_pending_move_retries,
				message=str(exc),
				action="wait_for_next_state_and_retry",
			)

			if pending_move_retries > max_pending_move_retries:
				logger.log(
					"game",
					"move_post_retries_exhausted",
					game_id=game_id,
					bestmove=bestmove,
					retry_count=pending_move_retries,
				)
				raise

			continue


def main() -> int:
	arg_parser = argparse.ArgumentParser(description="Run Springer as a single-game Lichess bot challenger.")
	arg_parser.add_argument("--token", required=True, help="Lichess bot API token")
	arg_parser.add_argument("--config", default="lichess_config.toml", help="Path to lichess config TOML")
	args = arg_parser.parse_args()

	token = args.token.strip()
	if not token:
		print("Failed to load token: --token is empty", file=sys.stderr)
		return 1

	config_path = args.config
	try:
		cfg = load_config(config_path)
	except Exception as exc:
		print(f"Failed to load config: {exc}", file=sys.stderr)
		return 1

	logger = JsonlLogger(cfg.log_file, also_stdout=True)
	logger.log("app", "start", config_path=config_path)

	client = LichessClient(cfg, logger, token)
	engine = UciEngine(cfg, logger)
	active_game: Dict[str, Optional[str]] = {"id": None}
	active_game_lock = threading.Lock()

	if not os.path.exists(cfg.engine_path):
		logger.log("app", "error", message="Engine executable not found", engine_path=cfg.engine_path)
		return 1

	def resign_active_game() -> None:
		game_id_local: Optional[str]
		with active_game_lock:
			game_id_local = active_game["id"]
		if not game_id_local:
			logger.log("operator", "resign_ignored", reason="no_active_game")
			return
		client.resign_game(game_id_local)
		logger.log("operator", "resign_sent", game_id=game_id_local)

	operator_control = OperatorControl(logger, resign_active_game)
	operator_control.start()

	try:
		account = client.get_account()
		our_username = str(account.get("username", ""))
		logger.log("app", "account", username=our_username)

		if not our_username:
			logger.log("app", "error", message="Unable to resolve account username")
			return 1

		challenge_resp = client.create_challenge()
		challenge_id = challenge_resp.get("id")
		if not challenge_id:
			challenge = challenge_resp.get("challenge", {})
			if isinstance(challenge, dict):
				challenge_id = challenge.get("id")
		if not challenge_id:
			logger.log("challenge", "error", message="Challenge response missing challenge.id", response=challenge_resp)
			return 1

		logger.log("challenge", "created", challenge_id=challenge_id, target=cfg.target_username)

		game_id = wait_for_challenge_result(client, logger, challenge_id)
		if not game_id:
			logger.log("app", "done", outcome="challenge_not_accepted")
			return 0

		with active_game_lock:
			active_game["id"] = game_id

		engine.start()
		engine.init_uci()
		play_single_game(client, engine, logger, game_id, our_username, cfg)
		logger.log("app", "done", outcome="game_finished", game_id=game_id)
		return 0

	except KeyboardInterrupt:
		logger.log("app", "interrupt", signal="SIGINT", message="CTRL+C received")
		try:
			resign_active_game()
		except Exception as resign_exc:
			logger.log("app", "resign_on_interrupt_error", message=str(resign_exc))
		return 130

	except requests.HTTPError as exc:
		logger.log("app", "http_error", message=str(exc))
		return 1
	except Exception as exc:
		logger.log("app", "error", message=str(exc))
		return 1
	finally:
		with active_game_lock:
			active_game["id"] = None
		operator_control.stop()
		try:
			engine.stop()
		except Exception as exc:
			logger.log("app", "engine_stop_error", message=str(exc))


if __name__ == "__main__":
	raise SystemExit(main())
