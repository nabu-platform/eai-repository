# Java Integration

There is deep integration with java beans and java methods, more specifically lambda-friendly interfaces (no generics though).

## Interesting Interfaces

### Server

- **be.nabu.eai.services.api.FlatServiceTracker**: it is a flat interface that does more or less the same as be.nabu.libs.services.vm.api.VMServiceRuntimeTracker but in a flat method-driven way instead of requiring 10 method implementations. It can be used with **nabu.utils.Runtime.registerServiceTracker** to register a flow-based service tracker.

Note that the service tracker can also be registered on web artifacts.

### Authentication API

The [authentication api](https://github.com/nablex/authentication-api) can be implemented using these interfaces:

- **be.nabu.eai.authentication.api.PasswordAuthenticator.authenticate**: a custom EAI authenticator that requires username & password
- **be.nabu.eai.authentication.api.SecretAuthenticator.authenticate**: a custom EAI authenticator that requires a secret

These two interfaces are combined to implement **be.nabu.libs.authentication.api.Authenticator**.

You could also directly implement **be.nabu.libs.authentication.api.Authenticator.authenticate** but the credentials are plain java.security.Principal implementations where you will have to check which type it is.

Other interfaces come directly from the authentication API:

- **be.nabu.libs.authentication.api.PermissionHandler.hasPermission**: check if a token has a certain permission
- **be.nabu.libs.authentication.api.RoleHandler.hasRole**: check if a token has the indicated roles
- **be.nabu.libs.authentication.api.TokenValidator.isValid**: check if the token is still valid

### Plain Java

- **java.util.Comparator.compare**: this can be implemented if you want the ability to use default java compare logic. For example nabu.utils.List.sort uses this
