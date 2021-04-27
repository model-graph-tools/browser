FROM nginx:latest

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

ENV MGT_API localhost:9911
COPY src/main/nginx/default.conf.template /etc/nginx/templates/
COPY build/distributions /usr/share/nginx/html
