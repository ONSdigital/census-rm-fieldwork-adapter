# census-rm-fieldwork-adapter
Adapter service to convert from JSON events to the XML format required by Fieldwork Management Tool (FWMT).

# Queues

The adapter service connects to the following queues and exhanges.

 Queue | Exchange | Direction 
-------|----------|----------
|Action.Field | action-outbound-exchange | Input |
|FieldworkAdapter.invalidAddress| events | Input |
|FieldworkAdapter.Refusals | events | Input |
|FieldworkAdapter.uacUpdated | events | Input|
|RM.Field                           | adapter-outbound-exchange | Output|

# Overview

This service adapts internal JSON format messages into the FWMT XML format.
It is a simple service listening to various input events and translating them for the FWMT.


# How to configure


# How to test