#!/usr/bin/env python3

import argparse
import json
import ssl
from urllib.parse import quote
import urllib.request

FHIR_BASE = "https://hapi.fhir.org/baseR4"
TEMPERATURE_CODE = "http://loinc.org|8310-5"


def build_ssl_context(insecure: bool) -> ssl.SSLContext:
    if insecure:
        return ssl._create_unverified_context()
    try:
        import certifi  # type: ignore

        return ssl.create_default_context(cafile=certifi.where())
    except ImportError:
        return ssl.create_default_context()


def request_json(url: str, *, insecure: bool = False) -> dict:
    request = urllib.request.Request(url, method="GET")
    request.add_header("Accept", "application/fhir+json")

    with urllib.request.urlopen(request, timeout=20, context=build_ssl_context(insecure)) as response:
        return json.loads(response.read().decode("utf-8"))


def render_observation(entry: dict) -> str:
    resource = entry.get("resource", {})
    effective = resource.get("effectiveDateTime", "n/a")
    quantity = resource.get("valueQuantity", {})
    value = quantity.get("value", "n/a")
    unit = quantity.get("unit", "")
    observation_id = resource.get("id", "n/a")
    return f"- {effective} | {value} {unit} | Observation/{observation_id}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Fetch body temperature observations for a patient.")
    parser.add_argument("patient_id", help="FHIR patient id, for example 52960712")
    parser.add_argument("--raw", action="store_true", help="Print raw FHIR Bundle JSON")
    parser.add_argument("--insecure", action="store_true", help="Disable TLS certificate verification for local testing only.")
    args = parser.parse_args()

    query = (
        f"{FHIR_BASE}/Observation?patient={quote(args.patient_id)}"
        f"&code={quote(TEMPERATURE_CODE)}&_sort=-date&_count=20"
    )
    bundle = request_json(query, insecure=args.insecure)

    if args.raw:
        print(json.dumps(bundle, ensure_ascii=False, indent=2))
        return

    entries = bundle.get("entry", [])
    if not entries:
        print("No temperature observations found.")
        return

    print(f"Temperature observations for Patient/{args.patient_id}")
    for entry in entries:
        print(render_observation(entry))


if __name__ == "__main__":
    main()
