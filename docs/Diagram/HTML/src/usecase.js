// Use case diagram: actors are tall pills, associations are straight horizontal lines at fixed offsets.
const L = (id, y, t, extra) => ({ id, k: 'uc', x: 330, y, t, ...extra });
const R = (id, y, t, extra) => ({ id, k: 'uc', x: 870, y, t, ...extra });
const left = [['UC1', 'Sign up / sign in'], ['UC2', 'Manage profile & privacy'], ['UC3', 'Browse heritages & events'], ['UC4', 'View heritage preview'], ['UC5', 'Ask historical AI'], ['UC6', 'Open directions in map app'],
  ['UC11', 'View stamp collection'], ['UC12', 'Edit photo & add stickers'], ['UC13', 'Build journey album|and export it'], ['UC14', 'View journey recap']];
const ys = [90, 146, 202, 258, 314, 370, 486, 542, 598, 654];
const right = [['UC7', 'Arrive & unlock heritage', true], ['UC8', 'Photograph & recognize|a landmark', true], ['UC9', 'Unlock landmark|& collect stamp'], ['UC10', 'Pause, resume or|finish the visit']];
const CY = 372;
module.exports = {
  slug: 'use-case', width: 1360, height: 800, plain: true, termLabel: 'Actor',
  accentLabel: 'Core loop: arrive, then recognize',
  eyebrow: 'Use case diagram · Hanoi Lens', title: 'Who does what in Hanoi Lens',
  desc: 'Use case diagram: a signed-in Traveler uses account, discovery, visit and memory features; an Admin manages destinations and publishes events.',
  nodes: [
    { id: 'TRAV', k: 'term', x: 600, y: CY, w: 120, h: 604, t: 'Traveler', s: 'role USER' },
    { id: 'ADM', k: 'term', x: 1210, y: 514, w: 120, h: 100, t: 'Admin', s: 'role ADMIN' },
    ...left.map(([id, t], i) => L(id, ys[i], t)),
    ...right.map(([id, t, a], i) => R(id, ys[i], t, a ? { accent: true } : {})),
    R('UC15', 486, 'Manage destinations'), R('UC16', 542, 'Review & publish events'),
  ],
  edges: [
    ...left.map(([id], i) => ({ f: `TRAV.l${ys[i] - CY >= 0 ? '+' : ''}${ys[i] - CY}`, t: `${id}.r` })),
    ...right.map(([id], i) => ({ f: `TRAV.r${ys[i] - CY >= 0 ? '+' : ''}${ys[i] - CY}`, t: `${id}.l` })),
    { f: 'ADM.l-28', t: 'UC15.r' }, { f: 'ADM.l+28', t: 'UC16.r' },
  ],
  zones: [
    { label: 'Account & discover', nodes: ['UC1', 'UC2', 'UC3', 'UC4', 'UC5', 'UC6'] },
    { label: 'Remember', nodes: ['UC11', 'UC12', 'UC13', 'UC14'] },
    { label: 'Visit a heritage', nodes: ['UC7', 'UC8', 'UC9', 'UC10'] },
    { label: 'Administration', nodes: ['UC15', 'UC16'] },
  ],
};
