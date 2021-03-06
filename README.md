# census-rm-fieldwork-adapter
Adapter service to convert from JSON events to the XML format required by Fieldwork Management Tool (FWMT).

# Queues

The adapter service connects to the following queues via the exhanges listed.

|Queue | Exchange | Direction |
|------|----------|-----------|
|Action.Field | action-outbound-exchange | Input |
|FieldworkAdapter.caseUpdated | events | Input|
|RM.Field | adapter-outbound-exchange | Output|

# Overview

This service adapts internal JSON format messages into the FWMT XML format.
It is a simple service listening to various input events and translating them for the FWMT.

## Messages received

* Fieldwork FollowUp message is converted into ActionRequest.

* Case updated messages trigger a fieldwork message based on the `fieldDecision` in their metadata. If they do not contain a metadata block or `fieldDecision` they are ignored.

# How to configure

The default configuration is set for running the service locally. Essentially the HTTP connections to case-api and Exception manager should be:

```yaml
exceptionmanager:
  connection:
    scheme: http
    host: localhost
    port: 8666
```
# To debug census-rm-fieldwork-adapter locally

Start docker-dev

Stop census-rm-fieldwork-adapter running in Docker

```yaml
docker stop fwmt-adapter
```

Open the census-rm-fieldwork-adapter repository in IntelliJ.
Create a SpringBoot Run configuration called Application.
Run in debug mode.

# How to test

Use the [census-rm-acceptance-tests](https://github.com/ONSdigital/census-rm-acceptance-tests)