package push

import (
	"encoding/json"
	"fmt"
	"os"
)

type serviceAccount struct {
	ProjectID string `json:"project_id"`
}

func loadServiceAccount() (projectID string, jsonBytes []byte, err error) {
	if raw := os.Getenv("FCM_SERVICE_ACCOUNT_JSON"); raw != "" {
		jsonBytes = []byte(raw)
	} else if file := os.Getenv("FCM_SERVICE_ACCOUNT_FILE"); file != "" {
		jsonBytes, err = os.ReadFile(file)
		if err != nil {
			return "", nil, fmt.Errorf("read service account file: %w", err)
		}
	} else {
		return "", nil, fmt.Errorf("missing FCM_SERVICE_ACCOUNT_JSON or FCM_SERVICE_ACCOUNT_FILE")
	}

	var sa serviceAccount
	if err := json.Unmarshal(jsonBytes, &sa); err != nil {
		return "", nil, fmt.Errorf("parse service account json: %w", err)
	}
	if sa.ProjectID == "" {
		return "", nil, fmt.Errorf("service account missing project_id")
	}
	return sa.ProjectID, jsonBytes, nil
}
