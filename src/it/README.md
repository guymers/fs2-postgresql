./scripts/postgresql/server.sh

sbt example/run

./scripts/postgresql/notify.sh test msg1
./scripts/postgresql/notify.sh test msg2
./scripts/postgresql/notify.sh test1 msg1
./scripts/postgresql/notify.sh test msg3
