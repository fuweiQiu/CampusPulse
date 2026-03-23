import { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("campuspulse-token") || "");
  const [username, setUsername] = useState(() => localStorage.getItem("campuspulse-username") || "");
  const [displayName, setDisplayName] = useState(() => localStorage.getItem("campuspulse-display-name") || "");
  const [patientFhirId, setPatientFhirId] = useState(() => localStorage.getItem("campuspulse-patient-fhir-id") || "");

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

  const login = ({ token: nextToken, username: nextUsername, displayName: nextDisplayName, patientFhirId: nextPatientFhirId }) => {
    setToken(nextToken);
    setUsername(nextUsername);
    setDisplayName(nextDisplayName || "");
    setPatientFhirId(nextPatientFhirId || "");
  };

  const logout = () => {
    setToken("");
    setUsername("");
    setDisplayName("");
    setPatientFhirId("");
  };

  return (
    <AuthContext.Provider value={{ token, username, displayName, patientFhirId, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
