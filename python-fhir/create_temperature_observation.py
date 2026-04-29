#!/usr/bin/env python3

import argparse
import json
from datetime import datetime, timezone
import ssl
import urllib.request

FHIR_BASE = "https://hapi.fhir.org/baseR4"


def build_ssl_context(insecure: bool) -> ssl.SSLContext:
    if insecure:
        return ssl._create_unverified_context()
    try:
        import certifi  # type: ignore

        return ssl.create_default_context(cafile=certifi.where())
    except ImportError:
        return ssl.create_default_context()


def request_json(method: str, url: str, payload: dict | None = None, *, insecure: bool = False) -> dict:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(url, data=body, method=method)
    request.add_header("Accept", "application/fhir+json")
    request.add_header("Content-Type", "application/fhir+json; charset=UTF-8")
    request.add_header("Prefer", "return=representation")

    with urllib.request.urlopen(request, timeout=20, context=build_ssl_context(insecure)) as response:
        return json.loads(response.read().decode("utf-8"))


def build_observation(patient_id: str, temperature: float) -> dict:
    return {
        "resourceType": "Observation",
        "status": "final",
        "category": [
            {
                "coding": [
                    {
                        "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                        "code": "vital-signs",
                        "display": "Vital Signs",
                    }
                ],
                "text": "Vital Signs",
            }
        ],
        "code": {
            "coding": [
                {
                    "system": "http://loinc.org",
                    "code": "8310-5",
                    "display": "Body temperature",
                }
            ],
            "text": "Body temperature",
        },
        "subject": {"reference": f"Patient/{patient_id}"},
        "effectiveDateTime": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "valueQuantity": {
            "value": temperature,
            "unit": "C",
            "system": "http://unitsofmeasure.org",
            "code": "Cel",
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a temperature observation on a FHIR server.")
    parser.add_argument("patient_id", help="FHIR patient id, for example 52960712")
    parser.add_argument("--temperature", type=float, default=38.0, help="Temperature in Celsius")
    parser.add_argument("--insecure", action="store_true", help="Disable TLS certificate verification for local testing only.")
    args = parser.parse_args()

    observation = build_observation(args.patient_id, args.temperature)
    created_observation = request_json(
        "POST",
        f"{FHIR_BASE}/Observation",
        observation,
        insecure=args.insecure,
    )
    observation_id = created_observation["id"]

    print(f"Observation id: {observation_id}")
    print(f"Resource location: {FHIR_BASE}/Observation/{observation_id}")
    print(json.dumps(created_observation, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
