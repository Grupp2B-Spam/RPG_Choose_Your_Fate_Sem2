#!/usr/bin/env python3
"""Show the same choice event in both demo subscriber queues (without removing it)."""
import base64
import json
import urllib.request

creds = base64.b64encode(b"dev:dev-local-only").decode("ascii")
for queue in ("demo.storypath.choice-made", "demo.character.choice-made"):
    request = urllib.request.Request(
        f"http://localhost:15672/api/queues/%2F/{queue}/get",
        data=json.dumps({"count": 1, "ackmode": "ack_requeue_true", "encoding": "auto"}).encode(),
        method="POST",
        headers={"Authorization": f"Basic {creds}", "Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=5) as response:
        messages = json.load(response)
    if messages:
        print(queue, json.dumps(json.loads(messages[0]["payload"]), indent=2))
    else:
        print(queue, "EMPTY: submit a choice first")
