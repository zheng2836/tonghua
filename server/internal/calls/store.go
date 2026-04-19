package calls

import "sync"

type State string

const (
	StateInviting   State = "inviting"
	StateRinging    State = "ringing"
	StateConnecting State = "connecting"
	StateActive     State = "active"
	StateEnded      State = "ended"
	StateRejected   State = "rejected"
)

type Session struct {
	CallID       string `json:"callId"`
	CallerID     string `json:"callerId"`
	CalleeID     string `json:"calleeId"`
	CurrentState State  `json:"currentState"`
}

type Store struct {
	mu       sync.RWMutex
	sessions map[string]Session
}

func NewStore() *Store {
	return &Store{sessions: make(map[string]Session)}
}

func (s *Store) Put(session Session) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.sessions[session.CallID] = session
}

func (s *Store) Get(callID string) (Session, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	session, ok := s.sessions[callID]
	return session, ok
}

func (s *Store) UpdateState(callID string, state State) {
	s.mu.Lock()
	defer s.mu.Unlock()
	session, ok := s.sessions[callID]
	if !ok {
		return
	}
	session.CurrentState = state
	s.sessions[callID] = session
}

func (s *Store) List() []Session {
	s.mu.RLock()
	defer s.mu.RUnlock()
	result := make([]Session, 0, len(s.sessions))
	for _, session := range s.sessions {
		result = append(result, session)
	}
	return result
}
