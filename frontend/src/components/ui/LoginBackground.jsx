const NEBULAE = [
  { className: 'galaxy-nebula galaxy-nebula--1' },
  { className: 'galaxy-nebula galaxy-nebula--2' },
  { className: 'galaxy-nebula galaxy-nebula--3' },
  { className: 'galaxy-nebula galaxy-nebula--4' },
];

const STARS = [
  { top: '4%', left: '12%', size: 2, delay: 0 },
  { top: '7%', left: '28%', size: 1, delay: 1.2 },
  { top: '9%', left: '45%', size: 2, delay: 2.4 },
  { top: '6%', left: '62%', size: 1, delay: 0.8 },
  { top: '11%', left: '78%', size: 2, delay: 3.1 },
  { top: '14%', left: '91%', size: 1, delay: 1.7 },
  { top: '18%', left: '8%', size: 1, delay: 2.9 },
  { top: '21%', left: '22%', size: 2, delay: 0.5 },
  { top: '16%', left: '38%', size: 1, delay: 3.8 },
  { top: '19%', left: '55%', size: 2, delay: 1.1 },
  { top: '23%', left: '70%', size: 1, delay: 2.2 },
  { top: '17%', left: '85%', size: 2, delay: 4.1 },
  { top: '26%', left: '15%', size: 1, delay: 0.3 },
  { top: '29%', left: '33%', size: 2, delay: 2.7 },
  { top: '24%', left: '48%', size: 1, delay: 1.5 },
  { top: '31%', left: '63%', size: 2, delay: 3.3 },
  { top: '27%', left: '80%', size: 1, delay: 0.9 },
  { top: '34%', left: '6%', size: 2, delay: 2.1 },
  { top: '37%', left: '20%', size: 1, delay: 4.4 },
  { top: '32%', left: '42%', size: 2, delay: 1.8 },
  { top: '36%', left: '58%', size: 1, delay: 3.6 },
  { top: '39%', left: '74%', size: 2, delay: 0.6 },
  { top: '35%', left: '92%', size: 1, delay: 2.5 },
  { top: '44%', left: '11%', size: 1, delay: 3.9 },
  { top: '47%', left: '27%', size: 2, delay: 1.3 },
  { top: '42%', left: '51%', size: 1, delay: 0.2 },
  { top: '46%', left: '67%', size: 2, delay: 2.8 },
  { top: '43%', left: '83%', size: 1, delay: 4.2 },
  { top: '52%', left: '5%', size: 2, delay: 1.6 },
  { top: '55%', left: '18%', size: 1, delay: 3.4 },
  { top: '50%', left: '35%', size: 2, delay: 0.7 },
  { top: '54%', left: '72%', size: 1, delay: 2.3 },
  { top: '51%', left: '88%', size: 2, delay: 4.6 },
  { top: '58%', left: '14%', size: 1, delay: 1.9 },
  { top: '61%', left: '30%', size: 2, delay: 3.2 },
  { top: '56%', left: '46%', size: 1, delay: 0.4 },
  { top: '60%', left: '61%', size: 2, delay: 2.6 },
  { top: '57%', left: '77%', size: 1, delay: 4.8 },
  { top: '64%', left: '9%', size: 2, delay: 1.0 },
  { top: '67%', left: '24%', size: 1, delay: 3.7 },
  { top: '62%', left: '40%', size: 2, delay: 2.0 },
  { top: '66%', left: '56%', size: 1, delay: 4.0 },
  { top: '63%', left: '84%', size: 2, delay: 0.1 },
  { top: '70%', left: '16%', size: 1, delay: 2.9 },
  { top: '73%', left: '32%', size: 2, delay: 1.4 },
  { top: '68%', left: '49%', size: 1, delay: 3.5 },
  { top: '72%', left: '65%', size: 2, delay: 0.8 },
  { top: '69%', left: '90%', size: 1, delay: 2.4 },
  { top: '76%', left: '7%', size: 2, delay: 4.3 },
  { top: '79%', left: '21%', size: 1, delay: 1.7 },
  { top: '74%', left: '38%', size: 2, delay: 3.0 },
  { top: '78%', left: '54%', size: 1, delay: 0.5 },
  { top: '75%', left: '71%', size: 2, delay: 2.7 },
  { top: '81%', left: '86%', size: 1, delay: 4.5 },
  { top: '86%', left: '13%', size: 2, delay: 1.2 },
  { top: '89%', left: '29%', size: 1, delay: 3.8 },
  { top: '84%', left: '44%', size: 2, delay: 2.2 },
  { top: '88%', left: '60%', size: 1, delay: 0.9 },
  { top: '85%', left: '76%', size: 2, delay: 3.1 },
  { top: '92%', left: '52%', size: 1, delay: 1.8 },
  { top: '94%', left: '68%', size: 2, delay: 4.1 },
  { top: '91%', left: '82%', size: 1, delay: 2.5 },
];

export default function LoginBackground() {
  return (
    <div className="login-bg-ornaments" aria-hidden="true">
      <div className="galaxy-sky" />
      <div className="galaxy-spiral" />
      <div className="galaxy-dust" />

      {NEBULAE.map((nebula) => (
        <span key={nebula.className} className={nebula.className} />
      ))}

      {STARS.map((star, index) => (
        <span
          key={index}
          className={`galaxy-star${star.size > 1 ? ' galaxy-star--bright' : ''}`}
          style={{
            top: star.top,
            left: star.left,
            width: `${star.size}px`,
            height: `${star.size}px`,
            animationDelay: `${star.delay}s`,
          }}
        />
      ))}
    </div>
  );
}
