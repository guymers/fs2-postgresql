Start a PostgreSQL server:

    ./scripts/postgresql/server.sh

Start the example:

    sbt example/run

Trigger a notification with a key of `test`:

    ./scripts/postgresql/notify.sh test value
