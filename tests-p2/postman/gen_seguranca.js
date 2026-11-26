// Gera a coleção Postman de segurança básica do grupo P2 (NF-05 e NF-06, RNF-03).
// Uso: node gen_seguranca.js > P2_seguranca.postman_collection.json
// NF-05: back-end com a segurança desabilitada (padrão).
// NF-06: back-end com petclinic.security.enable=true.

// Hash BCrypt da senha "admin", o mesmo do usuário admin em db/h2/data.sql.
const ADMIN_HASH = '$2a$10$ymaklWBnpBKlgdMgkjWVF.GMGyvH8aDuTK.glFOaKw712LHtRRymS';

const test = lines => [{ listen: 'test', script: { type: 'text/javascript', exec: lines } }];
const basic = (user, pass) => ({ type: 'basic', basic: [{ key: 'username', value: user }, { key: 'password', value: pass }] });
const noauth = { type: 'noauth' };
const json = body => ({ mode: 'raw', raw: typeof body === 'string' ? body : JSON.stringify(body) });
const jsonHeader = [{ key: 'Content-Type', value: 'application/json' }];

// Verificações comuns de NF-05 para respostas de erro.
const noInternals = [
  "pm.test('Não é erro interno (status < 500)', () => pm.expect(pm.response.code).to.be.below(500));",
  "pm.test('Sem stack trace', () => pm.expect(pm.response.text()).to.not.match(/\\tat [a-z]+\\.|\\.java:\\d+\\)/));",
  "pm.test('Sem nome de pacote Java interno', () => pm.expect(pm.response.text()).to.not.match(/org\\.springframework|org\\.hibernate|java\\.lang|jakarta\\./));",
  "pm.test('Sem SQL ou detalhes do banco', () => pm.expect(pm.response.text()).to.not.match(/select .* from|insert into|H2|JDBC|SQLException/i));",
  "const t = pm.response.headers.get('Content-Type') || '';",
  "if (t.includes('json')) {",
  "  const title = pm.response.json().title || '';",
  "  pm.test('OBS-02 (informativo): title não expõe nome de classe Java [' + title + ']', () => pm.expect(title).to.not.match(/Exception$/));",
  "}",
];

const nf05 = [
  {
    name: 'NF-05a JSON malformado no cadastro de dono',
    event: test(["pm.test('Status 400', () => pm.response.to.have.status(400));", ...noInternals]),
    request: { method: 'POST', header: jsonHeader, body: json('{"firstName": "Ana", "lastName": '), url: '{{baseUrl}}/owners', auth: noauth },
  },
  {
    name: 'NF-05b Identificador com letras (GET /owners/abc)',
    event: test(["pm.test('Status 4xx', () => pm.expect(pm.response.code).to.be.within(400, 499));", ...noInternals]),
    request: { method: 'GET', url: '{{baseUrl}}/owners/abc', auth: noauth },
  },
  {
    name: 'NF-05c Dono inexistente (GET /owners/999999)',
    event: test(["pm.test('Status 404', () => pm.response.to.have.status(404));", ...noInternals]),
    request: { method: 'GET', url: '{{baseUrl}}/owners/999999', auth: noauth },
  },
  {
    name: 'NF-05d Tentativa de injeção SQL na busca por sobrenome',
    event: test([
      "pm.test('Status 200 ou 404, sem erro', () => pm.expect(pm.response.code).to.be.oneOf([200, 404]));",
      "pm.test('Não retorna todos os donos (a injeção não teve efeito)', () => { if (pm.response.code === 200) pm.expect(pm.response.json()).to.have.lengthOf(0); });",
      ...noInternals,
    ]),
    request: { method: 'GET', url: { raw: "{{baseUrl}}/owners?lastName=' OR '1'='1", host: ['{{baseUrl}}'], path: ['owners'], query: [{ key: 'lastName', value: "' OR '1'='1" }] }, auth: noauth },
  },
  {
    name: 'NF-05e Rota inexistente (GET /naoexiste)',
    event: test(["pm.test('Status 404', () => pm.response.to.have.status(404));", ...noInternals]),
    request: { method: 'GET', url: '{{baseUrl}}/naoexiste', auth: noauth },
  },
  {
    name: 'NF-05f Tipo de dado errado no corpo (telefone numérico como objeto)',
    event: test(["pm.test('Status 400', () => pm.response.to.have.status(400));", ...noInternals]),
    request: { method: 'POST', header: jsonHeader, body: json({ firstName: 'Ana', lastName: 'Teste', address: 'Rua A', city: 'Bauru', telephone: { numero: 1 } }), url: '{{baseUrl}}/owners', auth: noauth },
  },
];

const restricted = { username: 'p2vet', password: 'Senha123', enabled: true, roles: [{ name: 'ROLE_VET_ADMIN' }] };
const restrictedHash = { username: 'p2vethash', password: ADMIN_HASH, enabled: true, roles: [{ name: 'ROLE_VET_ADMIN' }] };

const nf06 = [
  {
    name: 'NF-06a Sem credenciais -> 401',
    event: test(["pm.test('Status 401', () => pm.response.to.have.status(401));"]),
    request: { method: 'GET', url: '{{baseUrl}}/owners', auth: noauth },
  },
  {
    name: 'NF-06b Senha errada -> 401',
    event: test(["pm.test('Status 401', () => pm.response.to.have.status(401));"]),
    request: { method: 'GET', url: '{{baseUrl}}/owners', auth: basic('admin', 'senhaerrada') },
  },
  {
    name: 'NF-06c Admin com perfil OWNER_ADMIN -> 200',
    event: test(["pm.test('Status 200', () => pm.response.to.have.status(200));"]),
    request: { method: 'GET', url: '{{baseUrl}}/owners', auth: basic('admin', 'admin') },
  },
  {
    name: 'NF-06d Admin cria usuário restrito p2vet (só VET_ADMIN) pela API',
    event: test([
      "pm.test('Status 201', () => pm.response.to.have.status(201));",
      "pm.test('Resposta não devolve a senha', () => pm.expect(pm.response.json()).to.not.have.property('password'));",
    ]),
    request: { method: 'POST', header: jsonHeader, body: json(restricted), url: '{{baseUrl}}/users', auth: basic('admin', 'admin') },
  },
  {
    name: 'NF-06e Usuário criado pela API consegue autenticar (GET /vets com p2vet -> 200)',
    event: test(["pm.test('Status 200 (autenticado e com o perfil VET_ADMIN)', () => pm.response.to.have.status(200));"]),
    request: { method: 'GET', url: '{{baseUrl}}/vets', auth: basic('p2vet', 'Senha123') },
  },
  {
    name: 'NF-06f Usuário restrito p2vet sem OWNER_ADMIN -> 403 em /owners',
    event: test(["pm.test('Status 403', () => pm.response.to.have.status(403));"]),
    request: { method: 'GET', url: '{{baseUrl}}/owners', auth: basic('p2vet', 'Senha123') },
  },
  {
    name: 'NF-06g Contorno: admin cria p2vethash com a senha já em BCrypt (hash de "admin")',
    event: test(["pm.test('Status 201', () => pm.response.to.have.status(201));"]),
    request: { method: 'POST', header: jsonHeader, body: json(restrictedHash), url: '{{baseUrl}}/users', auth: basic('admin', 'admin') },
  },
  {
    name: 'NF-06h p2vethash com perfil VET_ADMIN -> 200 em /vets',
    event: test(["pm.test('Status 200', () => pm.response.to.have.status(200));"]),
    request: { method: 'GET', url: '{{baseUrl}}/vets', auth: basic('p2vethash', 'admin') },
  },
  {
    name: 'NF-06i p2vethash sem OWNER_ADMIN -> 403 em /owners',
    event: test(["pm.test('Status 403', () => pm.response.to.have.status(403));"]),
    request: { method: 'GET', url: '{{baseUrl}}/owners', auth: basic('p2vethash', 'admin') },
  },
  {
    name: 'NF-06j p2vethash sem ADMIN -> 403 ao criar usuário',
    event: test(["pm.test('Status 403', () => pm.response.to.have.status(403));"]),
    request: { method: 'POST', header: jsonHeader, body: json({ username: 'p2intruso', password: 'x', enabled: true, roles: [{ name: 'ROLE_ADMIN' }] }), url: '{{baseUrl}}/users', auth: basic('p2vethash', 'admin') },
  },
];

const collection = {
  info: {
    name: 'P2 - Segurança básica (NF-05 e NF-06)',
    description: 'RNF-03. NF-05: executar com a segurança desabilitada (padrão), pasta "NF-05". NF-06: executar com petclinic.security.enable=true, pasta "NF-06". Gerada por gen_seguranca.js.',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  item: [
    { name: 'NF-05', description: 'Erros não expõem detalhes internos (segurança desabilitada).', item: nf05 },
    { name: 'NF-06', description: 'Autenticação e autorização por perfil (petclinic.security.enable=true).', item: nf06 },
  ],
};
process.stdout.write(JSON.stringify(collection, null, 2) + '\n');
