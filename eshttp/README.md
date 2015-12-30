# esc-eshttp
Event store commons HTTP adapter for Greg Young's [event store](https://www.geteventstore.com/).

## ESC Meta Data Structure
The common event store interface uses a special meta data structure for the [Event Store](https://geteventstore.com/).
This allows storing the content type, encoding and version of the user's data and meta data.  
```json
{
  "EscUserMeta": {
    "user": "michael"
  },
  "EscSysMeta": {
    "data-content-type": "application/json; encoding=UTF-8",
    "meta-content-type": "application/json; encoding=UTF-8",
    "meta-type":"MyMeta"
  }
}
```
Here is an example of XML content stored in a JSON format:
### Data
```json
{
  "Base64": "PGJvb2stYWRkZWQtZXZlbnQgbmFtZT0iU2hpbmluZyIgYXV0aG9yPSJTdGVwaGVuIEtpbmciLz4="
}
```
### Meta Data
```json
{
  "EscUserMeta": {
    "Base64": "PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+"
  },
  "EscSysMeta": {
    "data-content-type": "application/xml; encoding=UTF-8; transfer-encoding=base64",
    "meta-content-type": "application/xml; encoding=UTF-8; transfer-encoding=base64",
    "meta-type":"MyMeta"
  }
}
```
The actual data and meta data is base64 encoded. This way you can use any data format you like for storing your content.
