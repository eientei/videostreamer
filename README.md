# videostreamer
Simplistic nginx-rtmp ( https://github.com/arut/nginx-rtmp-module ) backed spring-boot application featuring low-latency streaming to RTMP-clients such as Adobe Flash Player.

### Bulding

```
mvn clean install
```

### Configuration
Done in application.properties file in current directory.
Sample configuration:
```
spring.jpa.database=POSTGRESQL
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.current_session_context_class=org.springframework.orm.hibernate4.SpringSessionContext
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://127.0.0.1/videostreamer
spring.datasource.username=videostreamer
spring.datasource.password=videostreamer
videostreamer.domain=eientei.org
videostreamer.smtpHost=eientei.org
videostreamer.rtmpPrefix=rtmp://127.0.0.1
videostreamer.captcha=true
videostreamer.captcha.secret=yourReCaptchaPrivateKey
videostreamer.captcha.public=yourReCaptchaPublicKey
```

### Running

```
java -jar target/videostreamer-1.0-SNAPSHOT.jar
```

### Sample nginx configuration
```
rtmp {
    server {
        listen 1935;
        ping 1m;
        chunk_size 32768;
        buflen 0s;
        application live {
            live on;
            publish_notify on;
            drop_idle_publisher 10s;
            play_restart on;

            wait_key on;
            wait_video on;
            interleave off;
            sync 10ms;
            
            on_publish http://127.0.0.1/backend/control/nginx/check_access;
            on_publish_done http://127.0.0.1/backend/control/nginx/finish_stream;
        }
    }
}
server {
  listen *:80;
  server_name video.eientei.org videodev.eientei.org;
  
        location / {
            rewrite ^/.*$ /index.html break;
            root   /home/http/video/static;
        }
        location ~ ^/(css|html|img|js|swf|favicon.ico|webjars) {
            root   /home/http/video/static;
        }

        location /backend {
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Server $host;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";

            proxy_pass http://127.0.0.1:8080/backend;
        }	
}
```
