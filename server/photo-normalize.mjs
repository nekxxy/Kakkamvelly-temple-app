import sharp from 'sharp';
/** Server-only normalization. Run in a restricted worker/container with CPU,
 * memory, filesystem and network limits before production activation. No SVG,
 * animations, URLs, user paths or output filenames accepted. No metadata kept. */
export async function normalizePhoto(input){
 if(!Buffer.isBuffer(input)||input.length===0||input.length>8*1024*1024)throw Error('Invalid image size');
 const options={limitInputPixels:24_000_000,failOn:'warning',sequentialRead:true,animated:false};
 const meta=await sharp(input,options).metadata();
 if(!['jpeg','png','webp'].includes(meta.format)||!meta.width||!meta.height||
   meta.width>10000||meta.height>10000||(meta.pages??1)!==1)throw Error('Unsupported image');
 const output=await sharp(input,options).rotate().resize({width:2560,height:2560,fit:'inside',withoutEnlargement:true})
   .flatten({background:'#ffffff'}).toColourspace('srgb').jpeg({quality:82,mozjpeg:false}).toBuffer();
 if(output.length>4*1024*1024)throw Error('Image output too large');
 return output;
}
