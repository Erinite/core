
Base functionality for Erinite application framework.

Erinite is a convenience layer on top of the excellent [Duct](https://github.com/duct-framework/duct) framework and is designed to support rapid application developement.

# Getting Started

The easiest way to get started is to install [erinite/cli](https://github.com/Erinite/cli) and run:

```
erin new <project-name>
```

You can then add modules, boundaries and services. For example:

```
cd <project-name>
erin add module --database sql
erin add boundary accounts --sql
erin add service accounts --add-boundary --db
```

You now have a project set up for SQL, with an `accounts` boundary and an `accounts` service:

```
src/<project-name>/boundaries/accounts.clj
src/<project-name>/boundaries/db/accounts.clj
src/<project-name>/boundaries/db/accounts.sql
src/<project-name>/services/accounts/core.clj
src/<project-name>/services/accounts/spec.clj
```

Be sure to edit `resources/config.edn` and `dev/resources/dev.edn` to setup your database connection.

## License

Copyright © 2019 Dan Kersten

Erinite is released under the terms of the [MIT License](https://github.com/Erinite/erinite-core/blob/master/LICENSE).
