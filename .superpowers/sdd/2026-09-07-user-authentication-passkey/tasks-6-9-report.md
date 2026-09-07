# Tasks 6–9 report

## Delivered

- Passkey registration ceremonies, registration verification, scoped credential listing, and idempotent ownership-checked deletion.
- Account-scoped Passkey authentication with dummy ceremonies for unavailable accounts and uniform authentication failures.
- Auth error contract integration, current-user endpoint, Android Digital Asset Links, and narrowed Word exception advice.
- OpenAPI annotations on the new REST endpoints.

## Files changed

- `src/main/java/cn/yanzongkeji/lawtest/user/application/exception/PasskeyConflictException.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyAuthenticationService.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyAuthenticationUseCase.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyManagementService.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyManagementUseCase.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyOptions.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyRegistrationService.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyRegistrationUseCase.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/AssetLinksController.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/PasskeyAuthenticationController.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/PasskeyRegistrationController.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/UserAuthenticationExceptionHandler.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/UserController.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasskeyAuthenticationOptionsRequest.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasskeyAuthenticationVerifyRequest.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasskeyRegistrationVerifyRequest.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/CurrentUserResponse.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/PasskeyOptionsResponse.java`
- `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/PasskeyResponse.java`
- `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/controller/WordQuestionExceptionHandler.java`

## Verification

`mvn -DskipTests clean compile` completed with `BUILD SUCCESS` on 2026-09-07T15:03:59+08:00. Maven compiled 123 production source files. Automated tests were not run by request.

Static checks confirmed all four authentication tables and constraints, stateless route protection with only documented public routes, no unfinished markers, and no secret values or token bodies exposed in logging/response object string forms. The configured development JWT fallback and Android development certificate placeholder remain explicitly rejected by the production profile validator.

## Limitations

No automated or integration tests were executed, as requested. Runtime database/browser ceremony behavior is therefore limited to compilation and static inspection in this batch.
