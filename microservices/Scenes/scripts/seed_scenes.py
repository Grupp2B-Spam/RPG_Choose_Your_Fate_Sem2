#!/usr/bin/env python3
"""Publish contract-shaped scene-content.updated events via the RabbitMQ management API."""
import base64
import json
import time
import urllib.error
import urllib.request

URL = "http://localhost:15672/api/exchanges/%2F/storycontent.events/publish"
CREDS = base64.b64encode(b"dev:dev-local-only").decode("ascii")
EVENTS = [
    {"eventId": "00000000-0000-4000-8000-000000000001", "eventType": "scene-content.updated",
     "occurredAt": "2026-11-27T12:00:00Z", "schemaVersion": 1,
     "payload": {"sceneId": "start", "sceneVersion": 1, "status": "PUBLISHED",
                 "chapterId": "chapter-1", "name": "Crossroads",
                 "description": "A path splits around an old tree.", "npcIds": [], "questIds": [],
                 "choices": [
                     {"choiceId": "left", "label": "Go toward the mill", "beat": "You go toward the mill.",
                      "destinationSceneId": "mill", "weight": 1, "grantedItemIds": []},
                     {"choiceId": "right", "label": "Go to the village", "beat": "You go to the village.",
                      "destinationSceneId": "village", "weight": 1, "grantedItemIds": []}
                 ]}},
    {"eventId": "00000000-0000-4000-8000-000000000002", "eventType": "scene-content.updated",
     "occurredAt": "2026-11-27T12:00:01Z", "schemaVersion": 1,
     "payload": {"sceneId": "mill", "sceneVersion": 1, "status": "PUBLISHED",
                 "chapterId": "chapter-1", "name": "Old Mill",
                 "description": "The wheel turns slowly.", "npcIds": [], "questIds": [], "choices": []}},
    {"eventId": "00000000-0000-4000-8000-000000000003", "eventType": "scene-content.updated",
     "occurredAt": "2026-11-27T12:00:02Z", "schemaVersion": 1,
     "payload": {"sceneId": "village", "sceneVersion": 1, "status": "PUBLISHED",
                 "chapterId": "chapter-1", "name": "Village",
                 "description": "A lantern glows by the inn.", "npcIds": [], "questIds": [], "choices": []}},
]

def publish(event):
    body = json.dumps({
        "properties": {"delivery_mode": 2, "content_type": "application/json"},
        "routing_key": "scene-content.updated",
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
            payload = event["payload"]
            print(f"Published {payload['sceneId']} version {payload['sceneVersion']}")
            break
        except (urllib.error.URLError, RuntimeError) as error:
            if attempt == 29:
                raise
            time.sleep(2)
