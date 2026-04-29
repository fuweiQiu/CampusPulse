import { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("campuspulse-token") || "");
  const [username, setUsername] = useState(() => localStorage.getItem("campuspulse-username") || "");
  const [displayName, setDisplayName] = useState(() => localStorage.getItem("campuspulse-display-name") || "");
  const [patientFhirId, setPatientFhirId] = useState(() => localStorage.getItem("campuspulse-patient-fhir-id") || "");
  const [patientResourceUrl, setPatientResourceUrl] = useState(
    () => localStorage.getItem("campuspulse-patient-resource-url") || "",
  );

  useEffect(() => {
    if (token) {
      localStorage.setItem("campuspulse-token", token);
    } else {
      localStorage.removeItem("campuspulse-token");
    }
  }, [token]);

  useEffect(() => {
    if (username) {
      localStorage.setItem("campuspulse-username", username);
    } else {
      localStorage.removeItem("campuspulse-username");
    }
  }, [username]);

  useEffect(() => {
    if (displayName) {
      localStorage.setItem("campuspulse-display-name", displayName);
    } else {
      localStorage.removeItem("campuspulse-display-name");
    }
  }, [displayName]);

  useEffect(() => {
    if (patientFhirId) {
      localStorage.setItem("campuspulse-patient-fhir-id", patientFhirId);
    } else {
      localStorage.removeItem("campuspulse-patient-fhir-id");
    }
  }, [patientFhirId]);

  useEffect(() => {
    if (patientResourceUrl) {
      localStorage.setItem("campuspulse-patient-resource-url", patientResourceUrl);
    } else {
      localStorage.removeItem("campuspulse-patient-resource-url");
    }
  }, [patientResourceUrl]);

  const login = ({
    token: nextToken,
    username: nextUsername,
    displayName: nextDisplayName,
    patientFhirId: nextPatientFhirId,
    patientResourceUrl: nextPatientResourceUrl,
  }) => {
    setToken(nextToken);
    setUsername(nextUsername);
    setDisplayName(nextDisplayName || "");
    setPatientFhirId(nextPatientFhirId || "");
    setPatientResourceUrl(nextPatientResourceUrl || "");
  };

  const logout = () => {
    setToken("");
    setUsername("");
    setDisplayName("");
    setPatientFhirId("");
    setPatientResourceUrl("");
  };

  return (
    <AuthContext.Provider
      value={{ token, username, displayName, patientFhirId, patientResourceUrl, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
