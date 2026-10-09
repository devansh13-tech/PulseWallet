import React, { useState, useEffect, useRef } from 'react';
import { aiAssistantApi, bankIngestionApi } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import './PulseAiAssistant.css';

const SAMPLE_PROMPTS = [
  'Can I afford a bike EMI?',
  'Why was my spending high in September?',
  'How do I reach my BMW goal sooner?',
  'spent 450 on lunch at Haldirams',
];

export default function PulseAiAssistant() {
  const { isAuthenticated } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [language, setLanguage] = useState('en'); // en, hi, mr
  const [messages, setMessages] = useState([
    {
      sender: 'ai',
      text: 'Namaste! I am your **Pulse AI Financial Assistant**. Ask me anything about your EMI affordability, spending trends, or goal acceleration. You can also type natural entries like *"spent 450 on lunch"* to record transactions!',
      timestamp: 'Just now',
    },
  ]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [stagedTxn, setStagedTxn] = useState(null);
  const [isRecording, setIsRecording] = useState(false);

  if (!isAuthenticated) return null;
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  async function handleSendMessage(textToSend) {
    const text = (textToSend || inputValue).trim();
    if (!text) return;

    const userMsg = {
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');
    setLoading(true);

    try {
      const response = await aiAssistantApi.chat({
        message: text,
        language,
        conversationHistory: messages.map((m) => `${m.sender}: ${m.text}`),
      });

      const aiMsg = {
        sender: 'ai',
        text: response.reply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        quickPrompts: response.suggestedQuickPrompts,
        stagedTxn: response.stagedTransaction,
      };

      if (response.stagedTransaction) {
        setStagedTxn(response.stagedTransaction);
      }

      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'ai',
          text: '⚠️ Unable to process your query right now. Please check connectivity or try again.',
          timestamp: 'Just now',
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  async function handleConfirmStagedTxn(txn) {
    if (!txn) return;
    try {
      await bankIngestionApi.batchImport({ transactions: [txn] });
      setStagedTxn(null);
      setMessages((prev) => [
        ...prev,
        {
          sender: 'ai',
          text: `🎉 **Successfully recorded!** ₹${txn.amount} for **${txn.merchant}** has been added to your ledger under **${txn.suggestedCategory}**.`,
          timestamp: 'Just now',
        },
      ]);
    } catch (err) {
      alert('Could not save transaction: ' + (err.message || 'Error'));
    }
  }

  function handleVoiceInput() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('Voice speech recognition is not supported in this browser. Please type your message.');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.lang = language === 'hi' ? 'hi-IN' : language === 'mr' ? 'mr-IN' : 'en-IN';
    recognition.interimResults = false;

    recognition.onstart = () => {
      setIsRecording(true);
    };

    recognition.onresult = (e) => {
      const transcript = e.results[0][0].transcript;
      setInputValue(transcript);
      setIsRecording(false);
    };

    recognition.onerror = () => {
      setIsRecording(false);
    };

    recognition.onend = () => {
      setIsRecording(false);
    };

    recognition.start();
  }

  return (
    <>
      {/* Floating Trigger Button */}
      <button
        type="button"
        className="pulse-ai-trigger-btn"
        onClick={() => setIsOpen(!isOpen)}
        title="Open Pulse AI Conversational Financial Assistant"
        id="pulse-ai-trigger"
      >
        <div className="pulse-ai-trigger-icon">🤖</div>
        <span className="pulse-ai-trigger-label">Ask Pulse AI</span>
        <span className="pulse-ai-live-beacon" />
      </button>

      {/* Floating Chat Modal */}
      {isOpen && (
        <div className="pulse-ai-chat-window" role="dialog" aria-modal="true" aria-label="Pulse AI Assistant">
          <div className="pulse-ai-header">
            <div className="pulse-ai-header-left">
              <div className="pulse-ai-avatar">🤖</div>
              <div>
                <strong>Pulse AI Assistant</strong>
                <span className="pulse-ai-status">● Live Data Connected</span>
              </div>
            </div>

            <div className="pulse-ai-header-right">
              {/* Language Selector */}
              <div className="pulse-ai-lang-select">
                <button
                  type="button"
                  className={language === 'en' ? 'active' : ''}
                  onClick={() => setLanguage('en')}
                  title="English"
                >
                  EN
                </button>
                <button
                  type="button"
                  className={language === 'hi' ? 'active' : ''}
                  onClick={() => setLanguage('hi')}
                  title="Hindi (हिंदी)"
                >
                  हिंदी
                </button>
                <button
                  type="button"
                  className={language === 'mr' ? 'active' : ''}
                  onClick={() => setLanguage('mr')}
                  title="Marathi (मराठी)"
                >
                  मराठी
                </button>
              </div>

              <button
                type="button"
                className="pulse-ai-close-btn"
                onClick={() => setIsOpen(false)}
                title="Close Assistant"
              >
                ✕
              </button>
            </div>
          </div>

          <div className="pulse-ai-body">
            {messages.map((m, idx) => (
              <div key={idx} className={`pulse-ai-bubble-row ${m.sender}`}>
                {m.sender === 'ai' && <div className="pulse-ai-mini-avatar">🤖</div>}
                <div className="pulse-ai-bubble">
                  <div
                    className="pulse-ai-text"
                    style={{ whiteSpace: 'pre-line' }}
                  >
                    {m.text}
                  </div>
                  <span className="pulse-ai-time">{m.timestamp}</span>

                  {/* Staged Transaction Card */}
                  {m.stagedTxn && (
                    <div className="pulse-ai-staged-card">
                      <div className="staged-card-badge">✨ Ready to Record</div>
                      <div className="staged-card-details">
                        <strong>₹{m.stagedTxn.amount}</strong> • {m.stagedTxn.merchant}
                        <span className="staged-category">{m.stagedTxn.suggestedCategory}</span>
                      </div>
                      <button
                        type="button"
                        className="staged-confirm-btn"
                        onClick={() => handleConfirmStagedTxn(m.stagedTxn)}
                      >
                        ✓ Confirm & Save Transaction
                      </button>
                    </div>
                  )}

                  {/* Quick follow up chips */}
                  {m.quickPrompts && (
                    <div className="pulse-ai-chips">
                      {m.quickPrompts.map((chip, cIdx) => (
                        <button
                          key={cIdx}
                          type="button"
                          className="pulse-ai-chip-btn"
                          onClick={() => handleSendMessage(chip)}
                        >
                          {chip}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}

            {loading && (
              <div className="pulse-ai-bubble-row ai">
                <div className="pulse-ai-mini-avatar">🤖</div>
                <div className="pulse-ai-bubble loading">
                  <span className="dot-pulse" /> Pulse AI is calculating your live numbers…
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts Bar */}
          <div className="pulse-ai-samples">
            <span className="samples-label">Try:</span>
            {SAMPLE_PROMPTS.slice(0, 2).map((prompt, i) => (
              <button
                key={i}
                type="button"
                className="sample-prompt-pill"
                onClick={() => handleSendMessage(prompt)}
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Input Bar */}
          <form
            className="pulse-ai-input-bar"
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
          >
            <input
              type="text"
              placeholder={
                language === 'hi'
                  ? 'पूछें: क्या मैं बाइक ईएमआई वहन कर सकता हूँ?'
                  : language === 'mr'
                  ? 'विचारा: मी बाईक ईएमआय घेऊ शकतो का?'
                  : 'Ask about EMI, spending, or type "spent 450 on lunch"...'
              }
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              disabled={loading}
            />

            <button
              type="button"
              className={`pulse-ai-mic-btn ${isRecording ? 'recording' : ''}`}
              onClick={handleVoiceInput}
              title="Voice input (Speech recognition)"
            >
              🎤
            </button>

            <button
              type="submit"
              className="pulse-ai-send-btn"
              disabled={loading || !inputValue.trim()}
              title="Send Message"
            >
              ➔
            </button>
          </form>
        </div>
      )}
    </>
  );
}
