#!/usr/bin/env python3

import argparse
import json
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


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a patient on a FHIR server.")
    parser.add_argument("--insecure", action="store_true", help="Disable TLS certificate verification for local testing only.")
    args = parser.parse_args()

    patient_payload = {
        "resourceType": "Patient",
        "active": True,
        "name": [
            {
                "use": "official",
                "text": "蔣小名",
                "family": "蔣",
                "given": ["小名"],
            }
        ],
        "gender": "male",
        "birthDate": "1967-01-01",
    }

    created_patient = request_json("POST", f"{FHIR_BASE}/Patient", patient_payload, insecure=args.insecure)
    patient_id = created_patient["id"]

    print(f"Patient id: {patient_id}")
    print(f"Resource location: {FHIR_BASE}/Patient/{patient_id}")
    print(json.dumps(created_patient, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
