#!/bin/sh

sudo ./garganttua-script-2.0.0-ALPHA01/uninstall.sh
rm -rf garganttua-script-2.0.0-ALPHA01
mvn clean package -am -Plinux-installer -Pdebug -DskipTests
tar -xvf ./target/garganttua-script-2.0.0-ALPHA01-linux-installer.tar.gz
sudo ./garganttua-script-2.0.0-ALPHA01/install.sh
