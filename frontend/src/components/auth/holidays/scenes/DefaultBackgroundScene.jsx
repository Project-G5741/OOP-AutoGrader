import React from 'react';

/**
 * Default Corporate EdTech 3D architectural backdrop planes (when no holiday is active).
 */
export default function DefaultBackgroundScene() {
  return (
    <>
      <div className="arch-grid" />
      <div className="arch-plane arch-plane--navy" />
      <div className="arch-plane arch-plane--cobalt" />
      <div className="arch-plane arch-plane--teal" />
      <div className="arch-ring arch-ring--top" />
      <div className="arch-ring arch-ring--bottom" />
    </>
  );
}
