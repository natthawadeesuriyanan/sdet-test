# Official Playwright Java image — version MUST match ui-tests/pom.xml's
# playwright.version (1.63.0), or Playwright won't find a matching browser
# binary. This image already includes JDK 17, Maven, and the Chromium/
# Firefox/WebKit browsers pre-installed, so no separate browser-install step
# is needed (unlike the CI workflow, which runs on a plain Ubuntu runner).
FROM mcr.microsoft.com/playwright/java:v1.63.0-noble

WORKDIR /app
COPY . .

ENV SAUCEDEMO_HEADLESS=true

# Standard "mvn clean test" works here because pom.xml's maven-clean-plugin
# is configured to clear target/'s contents rather than rmdir the directory
# itself — which would otherwise fail against the bind-mounted target/ in
# docker-compose.yml (an OS can't rmdir an active mount point from inside
# the container). See the plugin's comment in pom.xml for detail.
CMD ["mvn", "clean", "test"]
