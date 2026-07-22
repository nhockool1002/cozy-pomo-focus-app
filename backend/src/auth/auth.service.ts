import {
  ConflictException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService, JwtSignOptions } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';

const SALT_ROUNDS = 10;

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwt: JwtService,
    private readonly config: ConfigService,
  ) {}

  private async signTokens(userId: string, email: string) {
    const accessExpiresIn = this.config.get<string>('JWT_ACCESS_EXPIRES_IN') ?? '15m';
    const refreshExpiresIn = this.config.get<string>('JWT_REFRESH_EXPIRES_IN') ?? '30d';

    const [accessToken, refreshToken] = await Promise.all([
      this.jwt.signAsync(
        { sub: userId, email },
        {
          secret: this.config.get<string>('JWT_SECRET'),
          expiresIn: accessExpiresIn as JwtSignOptions['expiresIn'],
        },
      ),
      this.jwt.signAsync(
        { sub: userId, type: 'refresh' },
        {
          secret: this.config.get<string>('JWT_REFRESH_SECRET'),
          expiresIn: refreshExpiresIn as JwtSignOptions['expiresIn'],
        },
      ),
    ]);
    return { accessToken, refreshToken };
  }

  private sanitize(user: { passwordHash: string | null; [k: string]: unknown }) {
    const { passwordHash, ...rest } = user;
    return rest;
  }

  async register(dto: RegisterDto) {
    const existing = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (existing) {
      throw new ConflictException('Email đã được sử dụng');
    }

    const passwordHash = await bcrypt.hash(dto.password, SALT_ROUNDS);
    const user = await this.prisma.user.create({
      data: {
        email: dto.email,
        passwordHash,
        displayName: dto.displayName,
        authProvider: 'LOCAL',
        settings: { create: {} },
      },
    });

    const tokens = await this.signTokens(user.id, user.email);
    return { user: this.sanitize(user), ...tokens };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (!user || !user.passwordHash) {
      throw new UnauthorizedException('Sai email hoặc mật khẩu');
    }
    const valid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!valid) {
      throw new UnauthorizedException('Sai email hoặc mật khẩu');
    }

    const tokens = await this.signTokens(user.id, user.email);
    return { user: this.sanitize(user), ...tokens };
  }

  async refresh(refreshToken: string) {
    let payload: { sub: string; type: string };
    try {
      payload = await this.jwt.verifyAsync(refreshToken, {
        secret: this.config.get<string>('JWT_REFRESH_SECRET'),
      });
    } catch {
      throw new UnauthorizedException('Refresh token không hợp lệ hoặc đã hết hạn');
    }
    if (payload.type !== 'refresh') {
      throw new UnauthorizedException('Token không hợp lệ');
    }

    const user = await this.prisma.user.findUnique({ where: { id: payload.sub } });
    if (!user) {
      throw new UnauthorizedException('Người dùng không tồn tại');
    }

    return this.signTokens(user.id, user.email);
  }

  async me(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      throw new UnauthorizedException();
    }
    return this.sanitize(user);
  }
}
