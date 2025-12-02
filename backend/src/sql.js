const sql = require('pg');
const bc = require('bcrypt');


const pool = new sql.Pool({
    user: 'claudiom',
    password: 'n1j4M14D',
    host: 'localhost',
    port: 5432,
    database:'Teste'
});

const hora = async function () {
    //Mostra a hora da excucao do script, usado para testes
    const client =await pool.connect();
    try{
        const h = await client.query('SELECT NOW()');
        return h.rows[0];
    } catch(err){
        console.log(err);
    } finally{
        await client.release();
    }
}

const inserirLocal = async function (a,b) {
    //Recebe um objeto com: longitude, latitude, url da foto e descricao
    const client = await pool.connect();
    try {
        //Longitude primeiro, latitude segundo
        await client.query(`INSERT INTO locais (localizacao, url_foto, descricao, status,id_usuario_reportou) VALUES (ST_MakePoint($1, $2)::geography,
         $3, $4, 'pendente',$5)RETURNING id, status, data_reporte,ST_Y(localizacao::geometry) AS latitude, ST_X(localizacao::geometry)
         AS longitude;`,[a.longitude,a.latitude,a.url_foto,a.descricao,b]);
    } catch (err) {
        console.log(err);
        
    } finally{
        await client.release();
    }
    
}

const pendentes = async function () {
    //Mostra locais pendentes
    const client = await pool.connect();
    try {
        const p = await client.query(`SELECT id, descricao, url_foto, status, data_reporte, ST_Y(localizacao::geometry) AS latitude,
        ST_X(localizacao::geometry) AS longitude FROM locais WHERE status = 'pendente' ORDER BY data_reporte DESC;`);
        return p.rows;
    } catch (err) {
        console.log(err);
        
    } finally{
        await client.release();
    
    }
    
}

const limpar= async function (a,b) {
    //Atualiza o status do local para limpo
    const client = await pool.connect();
    try {
        await client.query(`UPDATE locais SET status='limpo',data_limpeza = CURRENT_TIMESTAMP, id_usuario_limpou = $2 WHERE id=$1 RETURNING id, status,
        data_limpeza;`,[a,b]);
    } catch (err) {
        console.log(err);
        
    }finally{
        await client.release();
    }
    
}

const inserirUsuario = async function(a) {
    const client = await pool.connect();
    const hashed = await bc.hash(a.senha,12);
    try {
        await client.query(`INSERT INTO usuarios (email, nome, password_hash) VALUES ($1, $2, $3) RETURNING id, email;`,
        [a.email,a.nome,hashed]);
    } catch (err) {
        console.log(err)
    } finally{
        await client.release();
    }
}

const usuarioExistente = async function (a) {
    const client = await pool.connect();
    try {
        const u = client.query('SELECT * FROM usuarios WHERE email = $1', [a.email]);
        return u;
    } catch (err) {
        console.log(err)
    } finally{
        await client.release();
    }
    
}

module.exports = {
    hora:hora,
    inserirLocal:inserirLocal,
    pendentes:pendentes,
    limpar:limpar,
    inserirUsuario:inserirUsuario,
    usuarioExistente: usuarioExistente
};