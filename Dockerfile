FROM alpine:3.19.1

ADD target/bundle /lrsql
ADD .java_modules /lrsql/.java_modules

# replace the linux runtime via jlink
RUN apk update \
        && apk upgrade \
        && apk add ca-certificates \
        && update-ca-certificates \
        && apk add --no-cache openjdk11 \
        && mkdir -p /lrsql/runtimes \
        && jlink --output /lrsql/runtimes/linux/ --add-modules $(cat /lrsql/.java_modules) \
        && apk del openjdk11 \
        && rm -rf /var/cache/apk/*

# delete bench utils for leaner container
RUN rm -rf /lrsql/bench*

WORKDIR /lrsql
EXPOSE 8080
EXPOSE 8443
#CMD ["/lrsql/bin/run_sqlite.sh"]
CMD ["/lrsql/bin/run_sqlite.sh", "-e", "LRSQL_API_KEY_DEFAULT=$LRSQL_API_KEY_DEFAULT", "-e", "LRSQL_API_SECRET_DEFAULT=$LRSQL_API_SECRET_DEFAULT", "-e", "LRSQL_ADMIN_USER_DEFAULT=$LRSQL_ADMIN_USER_DEFAULT", "-e", "LRSQL_ADMIN_PASS_DEFAULT=$LRSQL_ADMIN_PASS_DEFAULT", "-e", "LRSQL_DB_NAME_DEFAULT=$LRSQL_DB_NAME_DEFAULT"] 
