test-core:
	MSYS_NO_PATHCONV=1 docker run -v /var/run/docker.sock:/var/run/docker.sock -v .:/app -w /app --entrypoint /bin/sh  amazoncorretto:21-alpine -c 'cd core && ./gradlew --no-daemon test'

build:
	cd core && docker build -t leonpatmore2/spring-boot-faas .
