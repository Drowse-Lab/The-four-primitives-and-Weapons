const fs = require('fs');
const PNG = require('pngjs').PNG;
const dir = 'c:/Users/hrmcn/MCreatorWorkspaces/minecraft_armor_weapon/src/main/resources/assets/minecraft_armor_weapon/textures/item/';

function hex(h) {
  return [parseInt(h.slice(0,2),16), parseInt(h.slice(2,4),16), parseInt(h.slice(4,6),16), 255];
}
const T = [0,0,0,0];

function writePng(name, pixels) {
  const png = new PNG({width:16, height:16});
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const idx = (y*16+x)*4;
      const p = pixels[y][x];
      png.data[idx]   = p[0];
      png.data[idx+1] = p[1];
      png.data[idx+2] = p[2];
      png.data[idx+3] = p[3];
    }
  }
  fs.writeFileSync(dir + name + '.png', PNG.sync.write(png));
  console.log('Created ' + name + '.png');
}

// === ICE BOOK ===
{
  const OL = hex('1a2a35');
  const SP = hex('15202a');
  const SD = hex('243845');
  const C1 = hex('2a4050');
  const C2 = hex('355868');
  const C3 = hex('406878');
  const H1 = hex('80b8cc');
  const H2 = hex('60a0b8');
  const H3 = hex('a0d8e8');
  const H4 = hex('4890a8');
  const PD = hex('304858');
  const P1 = hex('90a8b0');
  const P2 = hex('a8c0c8');
  const P3 = hex('c0d8e0');
  const A1 = hex('40d0f0');
  const A2 = hex('80e8ff');
  const A3 = hex('c0f4ff');
  const A4 = hex('20a0c8');
  const A5 = hex('60c8e0');

  writePng('ice_book', [
    [T,T,T,T,A4,A2,T,T,T,T,T,T,T,T,T,T],
    [T,T,T,A4,A5,A3,A2,T,OL,OL,OL,T,T,T,T,T],
    [T,T,A4,A1,A2,A3,OL,OL,C1,C2,C1,OL,T,T,T,T],
    [T,T,A4,A1,OL,OL,C1,C2,C3,C3,C3,C3,OL,T,T,T],
    [T,A4,OL,OL,C1,C3,C3,C3,H1,H4,C3,C3,C3,OL,T,T],
    [OL,OL,C1,C1,C3,C3,H2,C3,H4,C3,H3,C3,C3,C1,OL,T],
    [OL,C1,C1,C3,C3,H1,C3,H4,C3,H2,C3,C3,C3,C3,PD,OL],
    [OL,OL,C2,C3,C3,C3,H2,H3,H1,C3,C3,C3,PD,PD,P1,T],
    [OL,OL,P1,C2,C3,C3,C3,C3,C3,C3,PD,PD,P1,P2,P1,T],
    [SP,SD,P3,P2,C2,C1,C3,C3,PD,PD,P1,P2,P2,P1,SD,SD],
    [T,SP,SD,P3,P2,C2,PD,PD,P1,P2,P2,P1,SD,SD,SP,SP],
    [T,T,SP,SD,P3,P2,P1,P2,P2,P1,SD,SD,SP,SP,A4,T],
    [T,T,T,SP,SD,P3,P2,P1,SD,SD,SP,SP,A5,A3,A4,T],
    [T,T,T,T,SP,SD,SD,SD,SP,SP,A1,A2,A3,A4,T,T],
    [T,T,T,T,T,SP,SP,SP,T,T,A4,A4,A4,T,T,T],
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
  ]);
}

// === ELECTRIC BOOK ===
{
  const OL = hex('2a2810');
  const SP = hex('201e08');
  const SD = hex('3a3818');
  const C1 = hex('4a4520');
  const C2 = hex('5e5830');
  const C3 = hex('706828');
  const H1 = hex('e8e050');
  const H2 = hex('c8c040');
  const H3 = hex('f0e860');
  const H4 = hex('a8a030');
  const PD = hex('585228');
  const P1 = hex('b0a870');
  const P2 = hex('c8c090');
  const P3 = hex('e0d8b0');
  const A1 = hex('f0f030');
  const A2 = hex('ffff60');
  const A3 = hex('fffff0');
  const A4 = hex('c0b800');
  const A5 = hex('e0d820');

  writePng('electric_book', [
    [T,T,T,T,T,A2,A3,T,T,T,T,T,T,T,T,T],
    [T,T,T,A4,A1,A2,A3,T,OL,OL,OL,T,T,T,T,T],
    [T,T,A4,A1,A5,A2,OL,OL,C1,C2,C1,OL,T,T,T,T],
    [T,T,A4,A1,OL,OL,C1,C2,C3,C3,C3,C3,OL,T,T,T],
    [T,A5,OL,OL,C1,C3,C3,C3,H1,H2,C3,C3,C3,OL,T,T],
    [OL,OL,C1,C1,C3,C3,H2,H4,H1,C3,C3,C3,C3,C1,OL,T],
    [OL,C1,C1,C3,C3,C3,H3,C3,C3,H2,H4,C3,C3,C3,PD,OL],
    [OL,OL,C2,C3,C3,H4,H2,H3,H1,C3,C3,C3,PD,PD,P1,T],
    [OL,OL,P2,C2,C3,C3,C3,C3,C3,C3,PD,PD,P1,P2,P1,T],
    [SP,SD,P3,P2,C2,C1,C3,C3,PD,PD,P1,P2,P2,P1,SD,SD],
    [T,SP,SD,P3,P2,C2,PD,PD,P1,P2,P2,P1,SD,SD,SP,SP],
    [T,T,SP,SD,P3,P2,P1,P2,P2,P1,SD,SD,SP,SP,T,T],
    [T,T,T,SP,SD,P3,P2,P1,SD,SD,SP,SP,A1,A4,T,T],
    [T,T,T,T,SP,SD,SD,SD,SP,SP,A5,A1,A2,A1,A4,T],
    [T,T,T,T,T,SP,SP,SP,T,T,A5,A4,A4,A1,A4,T],
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
  ]);
}

// === CORROSION BOOK ===
{
  const OL = hex('1a2018');
  const SP = hex('101808');
  const SD = hex('283820');
  const C1 = hex('2a3a22');
  const C2 = hex('384830');
  const C3 = hex('405838');
  const H1 = hex('80c848');
  const H2 = hex('60a038');
  const H3 = hex('a0e060');
  const H4 = hex('508830');
  const PD = hex('385028');
  const P1 = hex('889878');
  const P2 = hex('a0b090');
  const P3 = hex('c0d0b0');
  const A1 = hex('60d020');
  const A2 = hex('90e840');
  const A3 = hex('b0f060');
  const A4 = hex('40a010');
  const A5 = hex('50c018');

  writePng('corrosion_book', [
    [T,T,T,T,A4,T,T,T,T,T,T,T,T,T,T,T],
    [T,T,T,A4,A1,A5,T,T,OL,OL,OL,T,T,T,T,T],
    [T,T,A4,A1,A2,A3,OL,OL,C1,C2,C1,OL,T,T,T,T],
    [T,T,A4,A5,OL,OL,C1,C2,C3,C3,C3,C3,OL,T,T,T],
    [T,A4,OL,OL,C1,C3,C3,C3,H4,C3,C3,C3,C3,OL,T,T],
    [OL,OL,C1,C1,C3,C3,H2,C3,H4,C3,H1,C3,C3,C1,OL,T],
    [OL,C1,C1,C3,C3,H1,C3,H4,C3,H2,C3,C3,C3,C3,PD,OL],
    [OL,OL,C2,C3,C3,C3,H2,H3,H1,C3,C3,C3,PD,PD,P1,T],
    [OL,OL,P1,C2,C3,C3,C3,C3,C3,C3,PD,PD,P1,P2,P1,T],
    [SP,SD,P3,P2,C2,C1,C3,C3,PD,PD,P1,P2,P2,P1,SD,SD],
    [T,SP,SD,P3,P2,C2,PD,PD,P1,P2,P2,P1,SD,SD,SP,SP],
    [T,T,SP,SD,P3,P2,P1,P2,P2,P1,SD,SD,SP,SP,A4,T],
    [T,T,T,SP,SD,P3,P2,P1,SD,SD,SP,SP,A5,A2,A4,T],
    [T,T,T,T,SP,SD,SD,SD,SP,SP,A1,A2,A3,A4,T,T],
    [T,T,T,T,T,SP,SP,SP,T,T,A4,A4,A4,T,T,T],
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
  ]);
}

// === HOLY BOOK ===
{
  const OL = hex('3a3020');
  const SP = hex('282010');
  const SD = hex('483820');
  const C1 = hex('8a7850');
  const C2 = hex('a89068');
  const C3 = hex('c0a878');
  const H1 = hex('f8f0c0');
  const H2 = hex('e8d898');
  const H3 = hex('fff8d0');
  const H4 = hex('d8c888');
  const PD = hex('907858');
  const P1 = hex('d0c0a0');
  const P2 = hex('e0d8b8');
  const P3 = hex('f0e8d0');
  const A1 = hex('f0d040');
  const A2 = hex('f8e870');
  const A3 = hex('fff8a0');
  const A4 = hex('c0a020');
  const A5 = hex('e0c030');

  writePng('holy_book', [
    [T,T,T,T,A3,A2,A3,T,T,T,T,T,T,T,T,T],
    [T,T,T,A1,A3,A2,A1,T,OL,OL,OL,T,T,T,T,T],
    [T,T,A4,A1,A2,A3,OL,OL,C1,C2,C1,OL,T,T,T,T],
    [T,T,A4,A5,OL,OL,C1,C2,C3,C3,C3,C3,OL,T,T,T],
    [T,A4,OL,OL,C1,C3,C3,C3,H1,H4,C3,C3,C3,OL,T,T],
    [OL,OL,C1,C1,C3,C3,H2,C3,H4,C3,H1,C3,C3,C1,OL,T],
    [OL,C1,C1,C3,C3,H1,C3,H4,C3,H2,C3,C3,C3,C3,PD,OL],
    [OL,OL,C2,C3,C3,C3,H2,H3,H1,C3,C3,C3,PD,PD,P1,T],
    [OL,OL,P1,C2,C3,C3,C3,C3,C3,C3,PD,PD,P1,P2,P1,T],
    [SP,SD,P3,P2,C2,C1,C3,C3,PD,PD,P1,P2,P2,P1,SD,SD],
    [T,SP,SD,P3,P2,C2,PD,PD,P1,P2,P2,P1,SD,SD,SP,SP],
    [T,T,SP,SD,P3,P2,P1,P2,P2,P1,SD,SD,SP,SP,A4,T],
    [T,T,T,SP,SD,P3,P2,P1,SD,SD,SP,SP,A5,A3,A4,T],
    [T,T,T,T,SP,SD,SD,SD,SP,SP,A1,A2,A3,A4,T,T],
    [T,T,T,T,T,SP,SP,SP,T,T,A4,A4,A4,T,T,T],
    [T,T,T,T,T,T,T,T,T,T,T,T,T,T,T,T],
  ]);
}

console.log('All done!');
