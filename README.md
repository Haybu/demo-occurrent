### CQRS using Occurrent Event Sourcing Utilities

reference: https://occurrent.org/

- run the application
- create a new account by posting to http://localhost:8080/accounts with data
```
{
    "customerId":  10016,
    "amount": 1200.00
}
```
- view accounts snapshots at http://localhost:8080/snapshots