# IDGen

---

Generates unique ID's on demand

Takes inspiration from Discord and Twitter Snowflake id format

#### Info

```text
IDGen:

/docs:
  returns this documentation page
  
/:
  parameters:
    - n:
      - range [1,1000]
      - integer
  examples:
    - No n specified:
    { "id": "239578532651466752" }
    - n = 2:
    { "items": [ "239578501894635520", "239578532651466752" ] }
  returns a list of items (ids) if n is specified, otherwise a single id

```