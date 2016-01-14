#!/usr/bin/env python3

from subprocess import *
import os
import sys

HOST = "server.jaje.ninja"
USER = "morgaroth"
ADDRESS = "%s@%s" % (USER, HOST)
PORT = 28223
REMOTE_BACK_DIR = "/mnt/quide/back"
REMOTE_FRONT_DIR = "/mnt/quide/front"


def ssh(command):
    """
    :type command: str | list
    """
    if isinstance(command, str):
        command = command.split(' ')
    port_param = []
    if PORT is not None:
        port_param = ["-p%d" % PORT]
    out = check_output(['ssh', ADDRESS] + port_param + command,
                       universal_newlines=True)  # , stdin=PIPE, stdout=PIPE, close_fds=True)
    # p.wait()
    # if p.returncode != 0:
    #     raise Exception("command failed err %s, out %s" % (p.stderr, p.stdout))

    # return p.stdout
    return out


def ssh_list(directory: str):
    return ssh(['ls', '-l', directory])


def list_deploy_directory():
    # return [p.split(' ')[2] for p in ssh_list("/mnt/bots").split('\n') if not p.startswith("total")]
    return [
        f[0] for f in
        [p.split(' ')[-1:] for p in ssh_list("/mnt/bots").split('\n') if not p.startswith("total")]
        if len(f) > 0
        ]


def get_backend_artifact():
    jars = [f for f in os.listdir("./jvm/target/scala-2.11/") if f.endswith(".jar")]
    if len(jars) == 1:
        return './jvm/target/scala-2.11/%s' % jars[0]
    else:
        raise Exception("in jvm/target/scala-2.11/ should be only one jar, but there is %s" % str(jars))


scp_port_param = []
if PORT is not None:
    scp_port_param = ["-P %d" % PORT]

environment = os.environ.copy()
environment.update({'DEPLOY': 'true'})


def deploy_backend():
    print("Building backend app ...")
    Popen(["sbt", ";clean;server/clean;server/assembly"]).wait()
    file = get_backend_artifact()
    filename = os.path.basename(file)
    print("Deploying %s" % file)
    Popen(['scp'] + scp_port_param + [file, "%s:/%s" % (ADDRESS, REMOTE_BACK_DIR)]).wait()
    print("Deployed. Removing current link...")
    print(ssh(['rm', '-rf', "%s/quide-current.jar" % REMOTE_BACK_DIR]))
    print("Removed. Linking with new version...")
    print(ssh(['ln', '-s', '%s/%s' % (REMOTE_BACK_DIR, filename), '%s/quide-current.jar' % REMOTE_BACK_DIR]))
    print("Linked. Restarting supervisor/...")
    print(ssh(['supervisorctl', 'restart', 'quide']))
    print("Restarted.")


def deploy_frontend():
    print("Building frontent js...")
    # Popen(["sbt", ";clean;client/clean;fullOptJS"], env=environment).wait()
    print("Deploying site...")
    dir_ = ['scp', '-r'] + scp_port_param + ['./web/*', "%s:%s/" % (ADDRESS, REMOTE_FRONT_DIR)]
    Popen(' '.join(dir_), shell=True).wait()
    print("Deployed.")


def deploy():
    print("Building backend and frontend apps ...")
    Popen(["sbt", ";clean;server/clean;client/clean;server/assembly;client/fullOptJS"], env=environment).wait()
    file = get_backend_artifact()
    filename = os.path.basename(file)
    print("Deploying %s" % file)
    Popen(['scp'] + scp_port_param + [file, "%s:/%s" % (ADDRESS, REMOTE_BACK_DIR)]).wait()
    print("Deployed. Removing current link...")
    print(ssh(['rm', '-rf', "%s/quide-current.jar" % REMOTE_BACK_DIR]))
    print("Removed. Linking with new version...")
    print(ssh(['ln', '-s', '%s/%s' % (REMOTE_BACK_DIR, filename), '%s/quide-current.jar' % REMOTE_BACK_DIR]))
    print("Linked. Restarting supervisor/...")
    print(ssh(['supervisorctl', 'restart', 'quide']))
    print("Restarted.")
    print("Deploying site...")
    Popen(['scp', '-r'] + scp_port_param + ['./web/*', "%s:%s/" % (ADDRESS, REMOTE_FRONT_DIR)], shell=True).wait()
    print("Deployed.")


if len(sys.argv) < 2:
    deploy()
elif sys.argv[1] == "front":
    deploy_frontend()
elif sys.argv[1] == "back":
    deploy_backend()
else:
    deploy()
