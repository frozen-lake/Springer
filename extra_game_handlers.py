"""Springer overrides for lichess-bot's extra_game_handlers.py hooks (copied into the image by the Dockerfile)."""
import argparse
import sys
from functools import cache

import yaml

from lib import model
from lib.lichess_types import OPTIONS_TYPE


def game_specific_options(game: model.Game) -> OPTIONS_TYPE:  # noqa: ARG001
    """Use the engine options from the config file for every game."""
    return {}


@cache
def rated_block_list() -> frozenset[str]:
    """Lowercased usernames from `challenge.rated_block_list` in the config lichess-bot was started with."""
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--config", default="./config.yml")
    config_path = parser.parse_known_args(sys.argv[1:])[0].config
    with open(config_path) as stream:
        config = yaml.safe_load(stream) or {}
    names = (config.get("challenge") or {}).get("rated_block_list") or []
    return frozenset(str(name).lower() for name in names)


def is_supported_extra(challenge: model.Challenge) -> bool:
    """
    Decline rated challenges from users on `challenge.rated_block_list` (casual challenges are unaffected).

    Keeps the owner's own accounts from playing rated games against the bot, which Lichess treats as boosting.
    If the list can't be read, the exception propagates and lichess-bot declines the challenge.
    """
    if not challenge.rated:
        return True
    return challenge.challenger.name.lower() not in rated_block_list()
