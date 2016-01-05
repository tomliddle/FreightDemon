#!/bin/bash

username=ubuntu
projectPath=/home/ubuntu/Server/ScalatraWebProject
server=amazon
port=22
startupScript=start.sh
shutdownScript=stop.sh

echo "shutting down $projectPath"

ssh -p $port -n ${username}@${server} "cd $projectPath; ./$shutdownScript;"

echo "Rsyncing with ${username}@${server}:${projectPath}"

rsync -avz --delete --rsh="ssh -p $port"  build/libs/ ${username}@${server}:${projectPath}  || exit 1

echo ""
echo "Rsynced with ${username}@${server}:${projectPath}"
echo "Starting $projectPath"

ssh -p $port -n ${username}@${server} "cd $projectPath; ./$startupScript > /dev/null"
