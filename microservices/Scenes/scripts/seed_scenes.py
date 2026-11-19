#!/usr/bin/env python3
"""Publish StoryContentManager-like scene.changed events via the RabbitMQ management API."""
import base64
import json
import time
import urllib.error
import urllib.request

URL = "http://localhost:15672/api/exchanges/%2F/game.events/publish"
CREDS = base64.b64encode(b"dev:dev-local-only").decode("ascii")
EVENTS = [
    {"sceneId": "start", "version": 1, "operation": "UPSERT", "title": "Crossroads",
     "text": "A path splits around an old tree.",
     "choices": [
         {"id": "left", "text": "Go toward the mill", "nextSceneId": "mill", "effects": {"courage": 1}},
         {"id": "right", "text": "Go to the village", "nextSceneId": "village", "effects": {}}
     ]},
    {"sceneId": "mill", "version": 1, "operation": "UPSERT", "title": "Old Mill",
     "text": "The wheel turns slowly.", "choices": []},
    {"sceneId": "village", "version": 1, "operation": "UPSERT", "title": "Village",
     "text": "A lantern glows by the inn.", "choices": []},
]

def publish(event):
    body = json.dumps({
        "properties": {"delivery_mode": 2, "content_type": "application/json"},
        "routing_key": "scene.changed",
        "payload": json.dumps(event),
        "payload_encoding": "string",
    }).encode("utf-8")
    request = urllib.request.Request(
        URL, data=body, method="POST", headers={
            "Authorization": f"Basic {CREDS}",
            "Content-Type": "application/json",
        })
    with urllib.request.urlopen(request, timeout=5) as response:
        result = json.load(response)
    if not result.get("routed"):
        raise RuntimeError("RabbitMQ did not route the scene event to the indexer queue")

for event in EVENTS:
    for attempt in range(30):
        try:
            publish(event)
            print(f"Published {event['sceneId']} version {event['version']}")
            break
        except (urllib.error.URLError, RuntimeError) as error:
            if attempt == 29:
                raise
            time.sleep(2)
