package ws

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/websocket"
	"tonghua/server/internal/calls"
	"tonghua/server/internal/signaling"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

func Handler(hub *Hub, store *calls.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := r.URL.Query().Get("userId")
		if userID == "" {
			http.Error(w, "missing userId", http.StatusBadRequest)
			return
		}

		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Printf("upgrade failed: %v", err)
			return
		}

		client := &Client{UserID: userID, Conn: conn}
		hub.Register(userID, client)
		defer func() {
			hub.Unregister(userID)
			_ = conn.Close()
		}()

		for {
			_, data, err := conn.ReadMessage()
			if err != nil {
				log.Printf("ws read error user=%s err=%v", userID, err)
				return
			}

			var msg signaling.Message
			if err := json.Unmarshal(data, &msg); err != nil {
				log.Printf("bad ws payload user=%s err=%v", userID, err)
				continue
			}

			if string(msg.Type) == "ping" {
				pong, _ := json.Marshal(map[string]any{
					"type": "pong",
					"callId": "",
					"data": map[string]string{},
				})
				_ = client.Send(pong)
				continue
			}

			if string(msg.Type) == "pong" {
				continue
			}

			applyState(store, userID, msg)

			targetUserID := msg.Data["targetUserId"]
			if targetUserID == "" {
				continue
			}

			if err := hub.SendTo(targetUserID, data); err != nil {
				log.Printf("ws forward failed from=%s to=%s err=%v", userID, targetUserID, err)
			}
		}
	}
}

func applyState(store *calls.Store, userID string, msg signaling.Message) {
	switch msg.Type {
	case signaling.EventCallInvite:
		store.Put(calls.Session{
			CallID:       msg.CallID,
			CallerID:     userID,
			CalleeID:     msg.Data["targetUserId"],
			CurrentState: calls.StateInviting,
		})
	case signaling.EventCallRinging:
		store.UpdateState(msg.CallID, calls.StateRinging)
	case signaling.EventCallAnswer:
		store.UpdateState(msg.CallID, calls.StateConnecting)
	case signaling.EventCallReject:
		store.UpdateState(msg.CallID, calls.StateRejected)
	case signaling.EventCallHangup, signaling.EventCallCancel:
		store.UpdateState(msg.CallID, calls.StateEnded)
	}
}
