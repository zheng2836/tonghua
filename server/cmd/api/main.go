package main

import (
    "encoding/json"
    "log"
    "net/http"

    "tonghua/server/internal/calls"
    "tonghua/server/internal/ws"
)

type healthResponse struct {
    OK bool `json:"ok"`
}

type deviceRegisterRequest struct {
    UserID   string `json:"userId"`
    DeviceID string `json:"deviceId"`
    FCMToken string `json:"fcmToken"`
}

func main() {
    mux := http.NewServeMux()
    callStore := calls.NewStore()
    hub := ws.NewHub()

    mux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Type", "application/json")
        _ = json.NewEncoder(w).Encode(healthResponse{OK: true})
    })

    mux.HandleFunc("/debug/calls", func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Type", "application/json")
        _ = json.NewEncoder(w).Encode(map[string]any{
            "ok": true,
            "calls": callStore.List(),
        })
    })

    mux.HandleFunc("/devices/register", func(w http.ResponseWriter, r *http.Request) {
        if r.Method != http.MethodPost {
            http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
            return
        }

        var req deviceRegisterRequest
        if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
            http.Error(w, "bad request", http.StatusBadRequest)
            return
        }

        w.Header().Set("Content-Type", "application/json")
        _ = json.NewEncoder(w).Encode(map[string]any{
            "ok": true,
            "deviceId": req.DeviceID,
        })
    })

    mux.HandleFunc("/ws", ws.Handler(hub, callStore))

    log.Println("tonghua api listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", mux))
}
