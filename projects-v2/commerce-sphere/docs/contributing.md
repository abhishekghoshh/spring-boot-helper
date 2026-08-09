# Contributing Guide

## How to Contribute

### Branch Strategy

- `master` — Production-ready code
- Feature branches: `feature/<description>` — New features
- Bug fix branches: `fix/<description>` — Bug fixes

### Development Workflow

1. Create a feature/fix branch from `master`
2. Implement your changes following the project conventions
3. Write tests for your changes
4. Ensure all tests pass
5. Update documentation if needed
6. Submit a pull request

### Pull Request Process

1. Ensure your branch is up to date with `master`
2. All tests must pass
3. PR description should explain what was changed and why
4. Link any related issues
5. Request review from maintainers

## Code Standards

### Java / Spring Boot

- Use constructor injection (never field injection)
- Use Java records for DTOs
- Use MapStruct for entity-to-DTO mapping
- Use Lombok for boilerplate reduction
- Follow SOLID principles
- Write unit tests for all service methods
- Update Swagger annotations for new endpoints
- Use `@ControllerAdvice` for global exception handling

### React / TypeScript

- Use function components and hooks (no class components)
- Use TanStack Query for server state management
- Use React Hook Form + Zod for form validation
- Keep components small and focused
- Extract reusable logic into custom hooks
- Use Material UI components consistently

### Testing

- Unit tests for services using Mockito
- Controller tests using `@WebMvcTest`
- Integration tests with Testcontainers
- Frontend tests using Vitest + React Testing Library

## Adding a New Backend Service

1. Create directory under `backend/<new-service>/`
2. Copy and adapt `pom.xml` from an existing service
3. Create the standard package structure (config, controller, dto, entity, repository, service, etc.)
4. Add Dockerfile following the multi-stage pattern
5. Add Helm chart under `helm/`
6. Add service to `docker-compose.yaml`
7. Add README.md with API documentation
8. Add API docs in `docs/api/`

## Adding a New Frontend Page

1. Create the page component in `src/pages/`
2. Add the route in `App.tsx`
3. Add API client module in `src/api/` if needed
4. Add custom hook in `src/hooks/` if needed
5. Add navigation link where appropriate
6. Write tests

## Documentation

Update documentation when:
- Adding new endpoints (update `docs/api/<service>.md`)
- Adding new features (update architecture doc if needed)
- Changing setup steps (update `docs/development.md`)
- Changing infrastructure (update `docker-compose.yaml`)

## Questions?

Open an issue or contact the maintainers.
