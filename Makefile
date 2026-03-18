.DEFAULT_GOAL := help

DOCKER_IMAGE := logflux-java-sdk-dev
DOCKER_RUN := docker run --rm -v $(PWD):/sdk -w /sdk $(DOCKER_IMAGE)

.PHONY: help docker-build build test clean shell publish-dry publish publish-tag

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

docker-build: ## Build the Docker dev image
	docker build -f Dockerfile.dev -t $(DOCKER_IMAGE) .

build: docker-build ## Build the SDK (in Docker)
	$(DOCKER_RUN) gradle build

test: docker-build ## Run tests (in Docker)
	$(DOCKER_RUN) gradle test

test-verbose: docker-build ## Run tests with verbose output (in Docker)
	$(DOCKER_RUN) gradle test --info

clean: ## Clean build artifacts
	rm -rf build/ .gradle/

shell: docker-build ## Open shell in Docker container
	docker run --rm -it -v $(PWD):/sdk -w /sdk $(DOCKER_IMAGE) sh

publish-dry: ## Dry-run publish to public repo
	./scripts/publish.sh --dry-run

publish: ## Publish to public repo
	./scripts/publish.sh

publish-tag: ## Publish with tag (TAG=v3.0.0)
	@test -n "$(TAG)" || (echo "Usage: make publish-tag TAG=v3.0.0" && exit 1)
	./scripts/publish.sh --tag $(TAG)
