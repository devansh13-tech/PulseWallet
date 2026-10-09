import { useEffect, useRef } from 'react';

/**
 * High-performance, GPU-accelerated interactive canvas animation.
 * Adapts dynamically based on the active theme:
 * - 'money': Floating gold coins, rupee notes, emerald sparks, and currency symbols.
 * - 'stock': Neon green & red candlesticks, bull energy waves, buy/sell arrows.
 * - 'nse': Dalal Street Indian Rupee (₹) glyphs, bull icons, and tri-color ambient aurora.
 * - 'bank': Holographic vault shields, digital circuits, platinum credit chips, and security nodes.
 */
export default function MoneyCanvasAnimation({ theme = 'money', enabled = true, subtle = true }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    if (!enabled) return;

    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    const handleResize = () => {
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    };
    window.addEventListener('resize', handleResize);

    // Mouse tracking for gentle particle interaction
    const mouse = { x: -1000, y: -1000, radius: 120 };
    const handleMouseMove = (e) => {
      mouse.x = e.clientX;
      mouse.y = e.clientY;
    };
    const handleMouseLeave = () => {
      mouse.x = -1000;
      mouse.y = -1000;
    };
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseleave', handleMouseLeave);

    // Theme particle configurations
    // Subtle mode uses fewer, softer particles so fintech data stays readable and professional
    const count = subtle ? 20 : 42;
    const particles = [];

    const getThemeConfig = (currentTheme) => {
      switch (currentTheme) {
        case 'stock':
          return {
            symbols: ['▲', '▼', '📈', '📊', '+2.4%', 'BULL'],
            colors: ['#00f59b', '#ff3b69', '#38bdf8', '#a855f7'],
            types: ['text', 'candlestick', 'spark'],
          };
        case 'nse':
          return {
            symbols: ['₹', '₹', 'NSE', 'BSE', '🐂', 'NIFTY'],
            colors: ['#f97316', '#22c55e', '#38bdf8', '#eab308'],
            types: ['text', 'coin', 'spark'],
          };
        case 'bank':
          return {
            symbols: ['🏛️', '💳', '🛡️', '₹', 'VAULT', '✦'],
            colors: ['#38bdf8', '#60a5fa', '#93c5fd', '#f8fafc'],
            types: ['text', 'shield', 'spark'],
          };
        case 'money':
        default:
          return {
            symbols: ['₹', '$', '€', '🪙', '💵', '💎', '✦'],
            colors: ['#10b981', '#f59e0b', '#34d399', '#fbbf24', '#6ee7b7'],
            types: ['text', 'coin', 'spark'],
          };
      }
    };

    const config = getThemeConfig(theme);

    // Initialize particles with soft alpha
    for (let i = 0; i < count; i++) {
      particles.push({
        x: Math.random() * width,
        y: Math.random() * height,
        vx: (Math.random() - 0.5) * 0.5,
        vy: -0.2 - Math.random() * 0.5, // Slow, peaceful drift
        size: subtle ? (11 + Math.random() * 14) : (14 + Math.random() * 18),
        symbol: config.symbols[Math.floor(Math.random() * config.symbols.length)],
        color: config.colors[Math.floor(Math.random() * config.colors.length)],
        alpha: subtle ? (0.07 + Math.random() * 0.18) : (0.15 + Math.random() * 0.45),
        rotation: Math.random() * Math.PI * 2,
        vRot: (Math.random() - 0.5) * 0.015,
        kind: config.types[Math.floor(Math.random() * config.types.length)],
        candleHeight: 14 + Math.random() * 18,
        candleColor: Math.random() > 0.4 ? '#00f59b' : '#ff3b69',
      });
    }

    // Render loop
    const render = () => {
      ctx.clearRect(0, 0, width, height);

      // Ambient theme background gradient flare
      const gradient = ctx.createRadialGradient(
        width * 0.8,
        height * 0.2,
        50,
        width * 0.8,
        height * 0.2,
        width * 0.6
      );

      if (theme === 'money') {
        gradient.addColorStop(0, 'rgba(16, 185, 129, 0.08)');
        gradient.addColorStop(0.5, 'rgba(245, 158, 11, 0.04)');
        gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
      } else if (theme === 'stock') {
        gradient.addColorStop(0, 'rgba(0, 245, 155, 0.07)');
        gradient.addColorStop(0.5, 'rgba(255, 59, 105, 0.04)');
        gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
      } else if (theme === 'nse') {
        gradient.addColorStop(0, 'rgba(249, 115, 22, 0.08)');
        gradient.addColorStop(0.5, 'rgba(34, 197, 94, 0.05)');
        gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
      } else {
        gradient.addColorStop(0, 'rgba(56, 189, 248, 0.08)');
        gradient.addColorStop(0.5, 'rgba(37, 99, 235, 0.04)');
        gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
      }

      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);

      // Render each particle
      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];

        // Update position
        p.x += p.vx;
        p.y += p.vy;
        p.rotation += p.vRot;

        // Mouse repulsion
        const dx = p.x - mouse.x;
        const dy = p.y - mouse.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < mouse.radius && dist > 0) {
          const force = (mouse.radius - dist) / mouse.radius;
          p.x += (dx / dist) * force * 3;
          p.y += (dy / dist) * force * 3;
        }

        // Screen wrap
        if (p.y < -40) {
          p.y = height + 30;
          p.x = Math.random() * width;
        }
        if (p.x < -40) p.x = width + 30;
        if (p.x > width + 40) p.x = -30;

        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.rotation);
        ctx.globalAlpha = p.alpha;

        if (p.kind === 'candlestick') {
          // Draw mini glowing stock candlestick
          ctx.strokeStyle = p.candleColor;
          ctx.fillStyle = p.candleColor;
          ctx.lineWidth = 1.5;

          // Wick
          ctx.beginPath();
          ctx.moveTo(0, -p.candleHeight / 2 - 4);
          ctx.lineTo(0, p.candleHeight / 2 + 4);
          ctx.stroke();

          // Body
          ctx.fillRect(-4, -p.candleHeight / 3, 8, (p.candleHeight * 2) / 3);
        } else if (p.kind === 'coin') {
          // Draw metallic coin disc with inner symbol
          ctx.beginPath();
          ctx.arc(0, 0, p.size * 0.75, 0, Math.PI * 2);
          ctx.fillStyle = p.color;
          ctx.shadowColor = p.color;
          ctx.shadowBlur = 10;
          ctx.fill();

          ctx.fillStyle = '#0f172a';
          ctx.font = `bold ${Math.round(p.size * 0.8)}px sans-serif`;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('₹', 0, 1);
        } else {
          // Draw text symbol (₹, 💵, 🐂, 🏛️, etc.)
          ctx.font = `bold ${Math.round(p.size)}px "Segoe UI Symbol", sans-serif`;
          ctx.fillStyle = p.color;
          ctx.shadowColor = p.color;
          ctx.shadowBlur = 12;
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText(p.symbol, 0, 0);
        }

        ctx.restore();
      }

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseleave', handleMouseLeave);
      cancelAnimationFrame(animationFrameId);
    };
  }, [theme, enabled]);

  if (!enabled) return null;

  return (
    <canvas
      ref={canvasRef}
      className="money-canvas-backdrop"
      style={{
        position: 'fixed',
        inset: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 0,
        opacity: subtle ? 0.65 : 0.95,
      }}
    />
  );
}
