package push

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"

	"golang.org/x/oauth2/google"
)

func (s *Sender) sendIncomingCallFCM(payload IncomingCallPayload) error {
	projectID, jsonBytes, err := loadServiceAccount()
	if err != nil {
		log.Printf("fcm dry-run: %v", err)
		return nil
	}

	ctx := context.Background()
	creds, err := google.CredentialsFromJSON(ctx, jsonBytes, "https://www.googleapis.com/auth/firebase.messaging")
	if err != nil {
		return fmt.Errorf("load google credentials: %w", err)
	}

	token, err := creds.TokenSource.Token()
	if err != nil {
		return fmt.Errorf("get oauth token: %w", err)
	}

	body, err := json.Marshal(map[string]any{
		"message": map[string]any{
			"token": payload.Token,
			"android": map[string]any{
				"priority": "high",
			},
			"data": map[string]string{
				"type":       "incoming_call",
				"callId":     payload.CallID,
				"callerId":   payload.CallerID,
				"callerName": payload.CallerName,
			},
		},
	})
	if err != nil {
		return fmt.Errorf("marshal fcm request: %w", err)
	}

	client := &http.Client{Timeout: 15 * time.Second}
	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", projectID)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("build fcm request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+token.AccessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("send fcm request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		respBody, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("fcm status=%d body=%s", resp.StatusCode, string(respBody))
	}

	return nil
}
