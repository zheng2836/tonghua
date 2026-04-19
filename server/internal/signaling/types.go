package signaling

type EventType string

const (
    EventCallInvite  EventType = "call.invite"
    EventCallRinging EventType = "call.ringing"
    EventCallAnswer  EventType = "call.answer"
    EventCallReject  EventType = "call.reject"
    EventCallCancel  EventType = "call.cancel"
    EventCallHangup  EventType = "call.hangup"
    EventWebRTCOffer EventType = "webrtc.offer"
    EventWebRTCAnswer EventType = "webrtc.answer"
    EventWebRTCIce EventType = "webrtc.ice"
)

type Message struct {
    Type   EventType         `json:"type"`
    CallID string            `json:"callId"`
    Data   map[string]string `json:"data,omitempty"`
}
