#!/bin/sh

java -Djava.library.path=lib -Djava.security.manager -Djava.security.auth.login.config=etc/login.config -Djava.security.policy=etc/policy -Dlog4j.configuration=file:./etc/log4j.properties -jar jdcp-console.jar
