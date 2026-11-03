#!/usr/bin/env python3
"""Non-blocking live source smoke-test for GitHub Actions.

Checks the exact endpoints the Android launcher uses. Prints no credentials
and does not treat an upstream outage as a build failure.
"""
from datetime import date
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
import json

BASE = "https://site.api.espn.com/apis/site/v2/sports/soccer/uefa.champions/scoreboard"
today = date.today()
urls = [
    ("ao-vivo", BASE),
    ("mes", BASE + "?dates=" + today.strftime("%Y%m") + "&limit=250"),
    ("dia", BASE + "?dates=" + today.strftime("%Y%m%d")),
    ("alternativa", "https://cdn.espn.com/core/uefa.champions/scoreboard?xhr=1&date=" + today.strftime("%Y%m%d")),
]
for label, uri in urls:
    req = Request(uri, headers={"Accept": "application/json", "User-Agent": "AuroraHome/1.9 (Android TV; public UCL scores)"})
    try:
        with urlopen(req, timeout=14) as r:
            obj = json.loads(r.read(2_000_001))
            if "events" in obj:
                ev = obj["events"]
            else:
                ev = obj.get("content",{}).get("sbData",{}).get("events",[])
            print(f"{label}: HTTP {r.status}, eventos={len(ev)}, campos={list(obj)[:5]}")
    except (HTTPError, URLError, ValueError, TimeoutError) as err:
        print(f"{label}: indisponivel: {type(err).__name__}: {err}")
