import { useEffect, useRef, useState } from "react";
import "../../styles/ScrollReveal.css";

export default function ScrollReveal({ as: Component = "div", className = "", delay = 0, children, ...props }) {
  const elementRef = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const motion = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (motion.matches || !("IntersectionObserver" in window)) {
      setVisible(true);
      return undefined;
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setVisible(true);
        observer.disconnect();
      }
    }, { threshold: 0, rootMargin: "0px 0px -32px 0px" });

    observer.observe(elementRef.current);
    const showWithReducedMotion = () => {
      if (motion.matches) {
        setVisible(true);
        observer.disconnect();
      }
    };
    motion.addEventListener("change", showWithReducedMotion);
    return () => {
      observer.disconnect();
      motion.removeEventListener("change", showWithReducedMotion);
    };
  }, []);

  return (
    <Component
      {...props}
      ref={elementRef}
      className={`scroll-reveal ${visible ? "is-visible" : ""} ${className}`}
      style={{ "--reveal-delay": `${delay}ms` }}
      onFocusCapture={() => setVisible(true)}
    >
      {children}
    </Component>
  );
}
