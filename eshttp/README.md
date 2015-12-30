# esc-eshttp
Event store commons HTTP adapter for Greg Young's [event store](https://www.geteventstore.com/).

## Meta data
The common event store interface uses a special meta data structure for the [Event Store](https://geteventstore.com/).
This allows storing the content type, encoding and version of the user's data and meta data.  
```json
{
  "EscUserMeta": {
    "user": "michael"
  },
  "EscSysMeta": {
    "data-content-type": "application/json; encoding=UTF-8",
    "meta-content-type": "application/json; encoding=UTF-8"
  }
}
```
