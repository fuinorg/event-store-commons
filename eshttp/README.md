# esc-eshttp
Event store commons HTTP adapter for Greg Young's [Event Store](https://www.geteventstore.com/).

### Requires an Event Store version >= 4.1.4

## ESC Meta Data Structure
The [Event Store](https://geteventstore.com/) has only limited support for different types of stored data. For example only UTF-8 encoding is supported if you want to use projections or the main data formats are JSON and (limited) XML. To allow having multiple types, different encodings and versions of data and meta data, the common event store interface introduces 
a separate meta data structure. It contains information about content-types, encoding, version, meta type and 
the meta information provided by the user.

Here is how the data structure looks like that is sent using the [Event Store Events Media Type](https://eventstore.com/docs/http-api/creating-writing-a-stream/index.html?tabs=tabid-1%2Ctabid-3%2Ctabid-5%2Ctabid-7%2Ctabid-17%2Ctabid-11%2Ctabid-13%2Ctabid-15#batch-writes):
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
	    "data-type":"BookAddedEvent",
            "data-content-type":"application/json; encoding=UTF-8",
	    "meta-type":"MyMeta",
            "meta-content-type":"application/json; encoding=UTF-8; version=3",
            "MyMeta":{
                "user":"michael"
            }
        }
    }
]
```
Some things to note:
- **EventId** Universally Unique Identifier (UUID) of the event (See [Event Store HTTP API](https://eventstore.com/docs/http-api/creating-writing-a-stream/index.html?tabs=tabid-1%2Ctabid-3%2Ctabid-5%2Ctabid-7%2Ctabid-17%2Ctabid-11%2Ctabid-13%2Ctabid-15)) 
- **Data** contains the user's event data (See [Event Store HTTP API](https://eventstore.com/docs/http-api/creating-writing-a-stream/index.html?tabs=tabid-1%2Ctabid-3%2Ctabid-5%2Ctabid-7%2Ctabid-17%2Ctabid-11%2Ctabid-13%2Ctabid-15))
- **MetaData** contains some additional information required by event store commons
- **data-type** is the unique type name of the event stored in the data element. It's actually a copy of the "EventType" field and only there to have complete meta information describing the data structure.
- **data-content-type** contains the mime-type, encoding and and an optional version of the data 
- **meta-type** is the unique type name of the meta data (equivalent to "EventType"/"data-type" for data)
- **meta-content-type** contains the mime-type, encoding and and an optional version of the meta data
- **MyMeta** is always the last optional element in the meta data structure and contains the user's meta data. The "meta-type" and "meta-content-type" are only available if there is such user meta data available. 

Here is another example of event (XML) and meta data (XML) with a different format than the surrounding JSON envelope:
```json
[
{
    "EventId":"bd58da40-9249-4b42-a077-10455b483c80",
    "EventType":"MyEvent",
    "Data": {
        "MyEvent":"PE15RXZlbnQ+PGlkPmJkNThkYTQwLTkyNDktNGI0Mi1hMDc3LTEwNDU1YjQ4M2M4MDwvaWQ+PGRlc2NyaXB0aW9uPkhlbGxvLCBYTUwhPC9kZXNjcmlwdGlvbj48L015RXZlbnQ+"   
    },
    "MetaData":{
        "data-type": "MyEvent",
        "data-content-type":"application/xml; version=1; encoding=utf-8; transfer-encoding=base64",
        "meta-type": "MyMeta",
        "meta-content-type":"application/xml; version=1; encoding=utf-8; transfer-encoding=base64",
        "MyMeta":"PG15LW1ldGE+PHVzZXI+YWJjPC91c2VyPjwvbXktbWV0YT4=" 
    }
}
]
```
The actual data and meta data is base64 encoded. This way you can use any data format you like for storing your content.
If you decode the base 64 data, it's "```<MyEvent><id>bd58da40-9249-4b42-a077-10455b483c80</id><description>Hello, XML!</description></MyEvent>```" (Data) and 
```<my-meta><user>abc</user></my-meta>``` (MyMeta).

