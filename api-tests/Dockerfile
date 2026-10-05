# No browser needed for API tests, so a plain Maven + JDK 17 image is enough
# (no need for the much larger Playwright image used by ui-tests).
FROM maven:3.9-eclipse-temurin-17

WORKDIR /app
COPY . .

# See ui-tests/pom.xml's maven-clean-plugin config and ui-tests/Dockerfile
# for why plain "mvn clean test" is safe here even with target/ bind-mounted.
CMD ["mvn", "clean", "test"]
