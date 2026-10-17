import * as cdk from 'aws-cdk-lib/core';
import * as autoscaling from 'aws-cdk-lib/aws-autoscaling';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

const INSTANCE_COUNT = 1;

const REPO = 'frozen-lake/Springer';
const REPO_URL = `https://github.com/${REPO}.git`;
const REPO_SUBJECT_PREFIX = 'repo:frozen-lake@108189602/Springer@839077294';
const DEPLOY_BRANCH = 'main';
const DEPLOY_DOCUMENT_NAME = 'springer-deploy';
const BOT_TAG_KEY = 'springer-role';
const BOT_TAG_VALUE = 'bot';
const TOKEN_PARAMETER = '/springer/lichess-bot-token';
const LOG_GROUP = '/springer/lichess-bot';
const UBUNTU_AMI_PARAMETER = '/aws/service/canonical/ubuntu/server/26.04/stable/current/arm64/hvm/ebs-gp3/ami-id';
const DEPLOY_TIMEOUT_SECONDS = 5400; // 90 min

class NetworkConstruct extends Construct {
  readonly vpc: ec2.Vpc;
  readonly securityGroup: ec2.SecurityGroup;

  constructor(scope: Construct, id: string) {
    super(scope, id);

    this.vpc = new ec2.Vpc(this, 'Vpc', {
      maxAzs: 2,
      natGateways: 0,
      subnetConfiguration: [{ name: 'public', subnetType: ec2.SubnetType.PUBLIC }],
    });

    this.securityGroup = new ec2.SecurityGroup(this, 'BotSecurityGroup', {
      vpc: this.vpc,
      description: 'Springer bot: no inbound, all outbound',
      allowAllOutbound: true,
    });
  }
}

interface BotConstructProps {
  vpc: ec2.IVpc;
  securityGroup: ec2.ISecurityGroup;
  instanceCount: number;
}

class BotConstruct extends Construct {
  constructor(scope: Construct, id: string, props: BotConstructProps) {
    super(scope, id);

    const logGroup = new logs.LogGroup(this, 'BotLogs', {
      logGroupName: LOG_GROUP,
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    const token = ssm.StringParameter.fromSecureStringParameterAttributes(this, 'Token', {
      parameterName: TOKEN_PARAMETER,
    });

    const role = new iam.Role(this, 'BotRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
      ],
    });
    token.grantRead(role);
    logGroup.grantWrite(role);

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      'set -euxo pipefail',
      'export HOME=/root DEBIAN_FRONTEND=noninteractive',
      'apt-get -o DPkg::Lock::Timeout=600 update',
      'apt-get -o DPkg::Lock::Timeout=600 install -y docker.io docker-buildx git',
      'snap wait system seed.loaded',
      'snap install aws-cli --classic',
      'export PATH="$PATH:/snap/bin"',
      `git clone ${REPO_URL} /srv/springer`,
      'cd /srv/springer',
      'bash run-aws.sh',
    );

    const launchTemplate = new ec2.LaunchTemplate(this, 'BotLaunchTemplate', {
      instanceType: new ec2.InstanceType('t4g.small'),
      machineImage: ec2.MachineImage.fromSsmParameter(UBUNTU_AMI_PARAMETER),
      userData,
      role,
      securityGroup: props.securityGroup,
      associatePublicIpAddress: true,
      requireImdsv2: true,
      cpuCredits: ec2.CpuCredits.UNLIMITED,
      blockDevices: [{
        deviceName: '/dev/sda1',
        volume: ec2.BlockDeviceVolume.ebs(16, {
          volumeType: ec2.EbsDeviceVolumeType.GP3,
          encrypted: true,
        }),
      }],
    });

    const group = new autoscaling.AutoScalingGroup(this, 'BotGroup', {
      vpc: props.vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      launchTemplate,
      minCapacity: props.instanceCount,
      maxCapacity: props.instanceCount,
    });
    cdk.Tags.of(group).add(BOT_TAG_KEY, BOT_TAG_VALUE, { applyToLaunchedInstances: true });
  }
}

class DeployConstruct extends Construct {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    const github = new iam.OidcProviderNative(this, 'GitHubOidc', {
      url: 'https://token.actions.githubusercontent.com',
      clientIds: ['sts.amazonaws.com'],
    });

    const deployRole = new iam.Role(this, 'DeployRole', {
      description: 'Assumed by GitHub Actions on the deploy branch to run the bot deploy document',
      maxSessionDuration: cdk.Duration.hours(2),
      assumedBy: new iam.WebIdentityPrincipal(github.oidcProviderArn, {
        StringEquals: {
          'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com',
          'token.actions.githubusercontent.com:sub': `${REPO_SUBJECT_PREFIX}:ref:refs/heads/${DEPLOY_BRANCH}`,
        },
      }),
    });

    new ssm.CfnDocument(this, 'DeployDocument', {
      name: DEPLOY_DOCUMENT_NAME,
      documentType: 'Command',
      content: {
        schemaVersion: '2.2',
        description: 'Pull the latest Springer and restart the bot. Waits for games in progress to finish.',
        mainSteps: [{
          action: 'aws:runShellScript',
          name: 'deploy',
          timeoutSeconds: DEPLOY_TIMEOUT_SECONDS,
          inputs: {
            runCommand: [
              'export PATH="$PATH:/snap/bin"', // Needed because run-aws.sh uses the AWS cli
              'cd /srv/springer && git pull --ff-only && bash run-aws.sh',
            ],
          },
        }],
      },
    });

    deployRole.addToPolicy(new iam.PolicyStatement({
      sid: 'SendDeployDocument',
      actions: ['ssm:SendCommand'],
      resources: [`arn:${cdk.Aws.PARTITION}:ssm:${cdk.Aws.REGION}:${cdk.Aws.ACCOUNT_ID}:document/${DEPLOY_DOCUMENT_NAME}`],
    }));

    deployRole.addToPolicy(new iam.PolicyStatement({
      sid: 'SendToBotInstances',
      actions: ['ssm:SendCommand'],
      resources: [`arn:${cdk.Aws.PARTITION}:ec2:${cdk.Aws.REGION}:${cdk.Aws.ACCOUNT_ID}:instance/*`],
      conditions: {
        StringEquals: { [`ssm:resourceTag/${BOT_TAG_KEY}`]: [BOT_TAG_VALUE] },
      },
    }));

    deployRole.addToPolicy(new iam.PolicyStatement({
      sid: 'ReadCommandResults',
      actions: ['ssm:GetCommandInvocation'],
      resources: ['*'],
    }));

    new cdk.CfnOutput(this, 'DeployRoleArn', {
      value: deployRole.roleArn,
      description: 'Set as the AWS_DEPLOY_ROLE_ARN repository variable in GitHub',
    });
  }
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const network = new NetworkConstruct(this, 'Network');
    new BotConstruct(this, 'Bot', {
      vpc: network.vpc,
      securityGroup: network.securityGroup,
      instanceCount: INSTANCE_COUNT,
    });
    new DeployConstruct(this, 'Deploy');
  }
}