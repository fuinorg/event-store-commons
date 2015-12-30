# esc-eshttp
Event store commons HTTP adapter for Greg Young's [Event Store](https://www.geteventstore.com/).

## ESC Meta Data Structure
The [Event Store](https://geteventstore.com/) has only limited support for different types of stored data. 
To allow storing different types and versions of data and meta data, the common event store interface introduces 
a separate meta data structure. It contains information about content-types, encoding, version, meta type and 
the meta information provided by the user.

Here is how the data structure looks like that is sent using the [Event Store Events Media Type](http://docs.geteventstore.com/http-api/3.4.0/writing-to-a-stream/):
```json
[
    {
        "EventId":"b3074933-c3ac-44c1-8854-04a21d560999",
        "EventType":"BookAddedEvent",
        "Data":{
            "name":"Shining",
            "author":"Stephen King"
        },
        "MetaData":{
            "EscUserMeta":{
                "user":"michael"
            },
            "EscSysMeta":{
                "data-content-type":"application/json; encoding=UTF-8",
                "meta-content-type":"application/json; encoding=UTF-8; version=3",
                "meta-type":"MyMeta"
            }
        }
    }
]
```
Some things to note:
- "Data" contains the user's data (event)
- "EscUserMeta" contains the user's meta data
- "EscSysMeta" has type informations about "Data" and "EscUserMeta"
- "*-content-type" contains the mime-type, encoding and and an optional version of the data structure
- "meta-type" is the unique type name of the meta data (equivalent to "EventType" for data)


Here is another example of event (XML) and mime type (TEXT) with a different format than the surrounding JSON envelope:
```json
[
    {
        "EventId":"b3074933-c3ac-44c1-8854-04a21d560999",
        "EventType":"BookAddedEvent",
        "Data":{
            "Base64": "PGJvb2stYWRkZWQtZXZlbnQgbmFtZT0iU2hpbmluZyIgYXV0aG9yPSJTdGVwaGVuIEtpbmciLz4="
        },
        "MetaData":{
            "EscUserMeta": {
                "Base64": "QW55dGhpbmcgZ29lcw=="
            },
            "EscSysMeta": {
                "data-content-type": "application/xml; encoding=UTF-8; transfer-encoding=base64",
                "meta-content-type": "text/plain; encoding=UTF-8; transfer-encoding=base64; version=2",
                "meta-type":"AnyMeta"
            }
        }
    }
]
```
The actual data and meta data is base64 encoded. This way you can use any data format you like for storing your content.
If you decode the base 64 data, it's ```<book-added-event name="Shining" author="Stephen King"/>``` (Data) and ```Anything goes``` (Meta).
